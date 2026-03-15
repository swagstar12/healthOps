from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import google.generativeai as genai
import httpx
import os
from dotenv import load_dotenv

load_dotenv()

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
JAVA_BACKEND_URL = os.getenv("JAVA_BACKEND_URL", "http://localhost:8080")

print("=== HealthOps Chatbot Starting ===")
print(f"GEMINI_API_KEY set: {bool(GEMINI_API_KEY)}")
print(f"GEMINI_API_KEY preview: {GEMINI_API_KEY[:10]}..." if GEMINI_API_KEY else "NO KEY FOUND")
print(f"JAVA_BACKEND_URL: {JAVA_BACKEND_URL}")

if GEMINI_API_KEY:
    genai.configure(api_key=GEMINI_API_KEY)

# Try models in order until one works
model = None
for model_name in ["gemini-1.5-flash", "gemini-1.5-flash-latest", "gemini-pro"]:
    try:
        model = genai.GenerativeModel(model_name)
        print(f"Successfully loaded model: {model_name}")
        break
    except Exception as e:
        print(f"Could not load {model_name}: {e}")

if model is None:
    print("WARNING: No Gemini model could be loaded!")

app = FastAPI(title="HealthOps Chatbot")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ─── Request / Response Models ─────────────────────────────────────────────────

class ChatRequest(BaseModel):
    message: str
    role: str
    token: str
    history: list[dict] = []


class ChatResponse(BaseModel):
    reply: str


# ─── Fetch live data from Java backend ─────────────────────────────────────────

async def fetch_backend(path: str, token: str):
    headers = {"Authorization": f"Bearer {token}"}
    async with httpx.AsyncClient(timeout=10) as client:
        try:
            resp = await client.get(
                f"{JAVA_BACKEND_URL}{path}", headers=headers
            )
            if resp.status_code == 200:
                return resp.json()
            else:
                print(f"  Backend {path} → {resp.status_code}")
        except Exception as e:
            print(f"  Error fetching {path}: {e}")
    return None


async def get_context_for_role(role: str, token: str) -> str:
    context_parts = []

    try:
        if role == "DOCTOR":
            availability = await fetch_backend("/api/doctor/availability", token)
            holidays     = await fetch_backend("/api/doctor/holidays", token)
            stats        = await fetch_backend("/api/doctor/dashboard/stats", token)
            patients     = await fetch_backend("/api/doctor/patients", token)

            if stats:
                context_parts.append(
                    f"Doctor Stats: totalPatients={stats.get('totalPatients')}, "
                    f"myVisits={stats.get('myVisitsCount')}, "
                    f"todayVisits={stats.get('todayVisits')}, "
                    f"myAppointments={stats.get('myAppointments')}, "
                    f"availabilitySlots={stats.get('availabilitySlots')}, "
                    f"upcomingHolidays={stats.get('upcomingHolidays')}"
                )

            days = ['', 'Monday', 'Tuesday', 'Wednesday',
                    'Thursday', 'Friday', 'Saturday', 'Sunday']
            if availability:
                slots = [
                    f"{days[int(a.get('dayOfWeek', 1))]} "
                    f"{a.get('startTime','?')}-{a.get('endTime','?')}"
                    for a in availability
                ]
                context_parts.append(
                    f"My Availability: "
                    f"{', '.join(slots) if slots else 'None set'}"
                )
            else:
                context_parts.append("My Availability: None set yet")

            if holidays:
                hols = [
                    f"{h.get('date','?')} - {h.get('reason','No reason')}"
                    for h in holidays
                ]
                context_parts.append(
                    f"My Holidays: {', '.join(hols)}"
                )
            else:
                context_parts.append("My Holidays: None set")

            if patients:
                patient_list = [
                    f"{p.get('code','?')} - {p.get('fullName','?')}"
                    for p in patients[:10]
                ]
                context_parts.append(
                    f"My Patients (first 10): {', '.join(patient_list)}"
                )
            else:
                context_parts.append("My Patients: None registered yet")

        elif role == "RECEPTIONIST":
            stats        = await fetch_backend("/api/reception/dashboard/stats", token)
            appointments = await fetch_backend("/api/reception/appointments", token)
            patients     = await fetch_backend("/api/reception/patients", token)
            doctors      = await fetch_backend("/api/reception/doctors", token)

            if stats:
                context_parts.append(
                    f"Reception Stats: "
                    f"totalPatients={stats.get('totalPatients')}, "
                    f"totalDoctors={stats.get('totalDoctors')}, "
                    f"todayAppointments={stats.get('todayAppointments')}, "
                    f"scheduled={stats.get('scheduledAppointments')}, "
                    f"completed={stats.get('completedAppointments')}, "
                    f"cancelled={stats.get('cancelledAppointments')}"
                )

            if appointments:
                scheduled = [
                    a for a in appointments
                    if a.get('status') == 'SCHEDULED'
                ][:5]
                appt_list = [
                    f"#{a['id']} {a['patient']['fullName']} with "
                    f"Dr.{a['doctor']['user']['fullName']} "
                    f"at {str(a.get('scheduledAt',''))[:16]}"
                    for a in scheduled
                ]
                context_parts.append(
                    f"Upcoming Scheduled Appointments: "
                    f"{'; '.join(appt_list) if appt_list else 'None'}"
                )
            else:
                context_parts.append("Appointments: None scheduled")

            if patients:
                context_parts.append(
                    f"Total registered patients: {len(patients)}"
                )

            if doctors:
                doc_list = [
                    f"Dr.{d['user']['fullName']} "
                    f"({d.get('specialization', 'General')})"
                    for d in doctors
                ]
                context_parts.append(
                    f"Available Doctors: {', '.join(doc_list)}"
                )

        elif role == "ADMIN":
            stats = await fetch_backend("/api/admin/dashboard/stats", token)
            if stats:
                context_parts.append(
                    f"Admin Stats: "
                    f"totalDoctors={stats.get('totalDoctors')}, "
                    f"totalReceptionists={stats.get('totalReceptionists')}, "
                    f"totalPatients={stats.get('totalPatients')}, "
                    f"totalAppointments={stats.get('totalAppointments')}, "
                    f"totalVisits={stats.get('totalVisits')}, "
                    f"todayAppointments={stats.get('todayAppointments')}, "
                    f"pendingAppointments={stats.get('pendingAppointments')}, "
                    f"completedAppointments={stats.get('completedAppointments')}"
                )

    except Exception as e:
        print(f"  Error building context for {role}: {e}")
        context_parts.append("Note: Some live data could not be fetched.")

    result = "\n".join(context_parts)
    print(f"  Context built ({len(result)} chars): {result[:150]}...")
    return result if result else "No live data available."


# ─── System prompt builder ──────────────────────────────────────────────────────

def build_system_prompt(role: str, live_context: str) -> str:
    base = (
        "You are HealthBot, a helpful AI assistant for Meera Multispecialty "
        "Hospital's internal management system called HealthOps.\n"
        "Be concise, friendly, and professional.\n"
        "Always use the live data provided below to give accurate answers.\n"
        "If asked to perform actions, guide the user step by step through the UI.\n"
        "Never say you cannot help — always try to assist.\n\n"
        f"=== LIVE SYSTEM DATA ===\n{live_context}\n=== END LIVE DATA ===\n\n"
    )

    role_prompts = {
        "DOCTOR": (
            "You are assisting a DOCTOR in the HealthOps system.\n"
            "Help them with: schedule/availability, holidays, patient records,\n"
            "creating visits with diagnosis/prescription/notes, and reports.\n\n"
            "UI NAVIGATION GUIDE:\n"
            "- View/add availability: Doctor Dashboard → Availability tab\n"
            "- View/add holidays: Doctor Dashboard → Holidays tab\n"
            "- Create a visit: Visits tab → select patient → fill form → Create Visit\n"
            "- View patients: Patients tab\n"
            "- Download reports: Reports tab\n"
            "- Dashboard stats: Dashboard tab\n"
        ),
        "RECEPTIONIST": (
            "You are assisting a RECEPTIONIST in the HealthOps system.\n"
            "Help them with: booking appointments, registering patients,\n"
            "managing doctor schedules, searching records, and downloading reports.\n\n"
            "UI NAVIGATION GUIDE:\n"
            "- Book appointment: Appointments tab → Create New Appointment "
            "→ select patient, doctor, date/time → Submit\n"
            "- Register patient: Patients tab → fill code/name/DOB/phone "
            "→ Add Patient\n"
            "- Update appointment status: Appointments tab → status dropdown "
            "(Scheduled/Completed/Cancelled)\n"
            "- Manage doctor schedule: Doctor Schedule tab → select doctor "
            "→ add availability or holiday\n"
            "- Search: Patients tab → use the search box\n"
            "- Reports: Reports tab → choose CSV type\n"
        ),
        "ADMIN": (
            "You are assisting an ADMIN in the HealthOps system.\n"
            "Help them with: system statistics, managing doctors and receptionists,\n"
            "user account controls (enable/disable), and overall oversight.\n\n"
            "UI NAVIGATION GUIDE:\n"
            "- Add doctor: Doctors tab → fill form → Add Doctor\n"
            "- Add receptionist: Receptionists tab → fill form → Add Receptionist\n"
            "- Disable/enable user: find user in list → click Disable or Enable\n"
            "- View all users: All Users tab\n"
            "- Dashboard stats: Dashboard tab\n"
            "- Delete doctor/receptionist: find in list → Delete button\n"
        ),
    }

    return base + role_prompts.get(role, "You are assisting a hospital staff member.")


# ─── Chat endpoint ──────────────────────────────────────────────────────────────

@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    print(f"\n--- New chat request ---")
    print(f"Role: {req.role}")
    print(f"Message: {req.message}")
    print(f"History turns: {len(req.history)}")

    # Validate API key
    if not GEMINI_API_KEY:
        print("ERROR: No Gemini API key!")
        raise HTTPException(
            status_code=500,
            detail="GEMINI_API_KEY is not set in environment variables"
        )

    # Validate model loaded
    if model is None:
        print("ERROR: No model loaded!")
        raise HTTPException(
            status_code=500,
            detail="Gemini model failed to load"
        )

    # Validate role
    if req.role not in ("ADMIN", "DOCTOR", "RECEPTIONIST"):
        raise HTTPException(status_code=400, detail=f"Invalid role: {req.role}")

    try:
        # 1. Fetch live data
        print("Fetching live context...")
        live_context = await get_context_for_role(req.role, req.token)

        # 2. Build system prompt
        system_prompt = build_system_prompt(req.role, live_context)

        # 3. Build Gemini history — THIS IS THE CRITICAL FIX
        # Gemini requires parts to be a list of dicts: [{"text": "..."}]
        gemini_history = []
        for turn in req.history[-10:]:   # keep last 10 turns only
            role_val = turn.get("role", "user")
            parts_val = turn.get("parts", "")

            # Normalize parts — could be string or already a list
            if isinstance(parts_val, list):
                parts = parts_val
            else:
                parts = [{"text": str(parts_val)}]

            gemini_history.append({
                "role": role_val,
                "parts": parts
            })

        # 4. Start Gemini chat session with history
        print(f"Starting Gemini chat (history: {len(gemini_history)} turns)...")
        chat_session = model.start_chat(history=gemini_history)

        # 5. Build the message to send
        # On first message include full system prompt as context
        # On follow-ups just send the message (history carries context)
        if not req.history:
            full_message = (
                f"{system_prompt}\n\n"
                f"The user's first question is: {req.message}"
            )
        else:
            full_message = req.message

        # 6. Send to Gemini
        print("Sending to Gemini API...")
        response = chat_session.send_message(full_message)
        reply = response.text

        print(f"Gemini response received ({len(reply)} chars)")
        print(f"Reply preview: {reply[:100]}...")

        return ChatResponse(reply=reply)

    except genai.types.BlockedPromptException as e:
        print(f"Blocked prompt: {e}")
        return ChatResponse(
            reply="I'm sorry, I couldn't process that request due to content policy. "
                  "Please rephrase your question."
        )
    except Exception as e:
        print(f"EXCEPTION TYPE: {type(e).__name__}")
        print(f"EXCEPTION MSG:  {str(e)}")
        # Return the actual error so you can see what's happening
        raise HTTPException(
            status_code=500,
            detail=f"{type(e).__name__}: {str(e)}"
        )


# ─── Health check ───────────────────────────────────────────────────────────────

@app.get("/health")
async def health():
    # Try a simple Gemini call to verify the key works
    gemini_status = "unknown"
    if GEMINI_API_KEY and model:
        try:
            test = model.generate_content("Say 'OK' in one word.")
            gemini_status = "connected"
        except Exception as e:
            gemini_status = f"error: {str(e)}"
    else:
        gemini_status = "no_key" if not GEMINI_API_KEY else "no_model"

    return {
        "status": "ok",
        "service": "HealthOps Chatbot",
        "gemini_key_set": bool(GEMINI_API_KEY),
        "gemini_status": gemini_status,
        "backend_url": JAVA_BACKEND_URL,
    }