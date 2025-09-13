import React, { useEffect, useState } from 'react'
import { api } from '../api'

type Patient = { id:number, code:string, fullName:string, phone?:string }
type Availability = { id:number, dayOfWeek:number, startTime:string, endTime:string }

export default function DoctorDashboard(){
  const [patients, setPatients] = useState<Patient[]>([])
  const [avail, setAvail] = useState<Availability[]>([])
  const [doctorId, setDoctorId] = useState<number>(1) // demo: use seeded doctor id 1 or fetch via /me

  async function load() {
    const ps = await api.get('/doctor/patients'); setPatients(ps.data)
    // in real app, get doctorId from backend based on current user
  }
  useEffect(()=>{ load() }, [])

  const [aForm, setAForm] = useState({ dayOfWeek:1, startTime:"09:00", endTime:"12:00" })
  async function addAvailability(e: React.FormEvent){
    e.preventDefault()
    const {data} = await api.post(`/doctor/availability/${doctorId}`, aForm)
    setAvail([...avail, data])
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Doctor Dashboard</h1>

      <div className="card">
        <h2 className="font-semibold mb-2">Patients</h2>
        <ul className="list-disc pl-5">
          {patients.map(p => <li key={p.id}>{p.code} — {p.fullName} — {p.phone}</li>)}
        </ul>
      </div>

      <div className="card">
        <h2 className="font-semibold mb-2">Add Availability</h2>
        <form className="grid grid-cols-4 gap-3" onSubmit={addAvailability}>
          <input className="input" type="number" min={1} max={7} value={aForm.dayOfWeek} onChange={e=>setAForm({...aForm, dayOfWeek:Number(e.target.value)})} placeholder="Day (1-7)" />
          <input className="input" value={aForm.startTime} onChange={e=>setAForm({...aForm, startTime:e.target.value})} />
          <input className="input" value={aForm.endTime} onChange={e=>setAForm({...aForm, endTime:e.target.value})} />
          <button className="btn">Add</button>
        </form>
        <ul className="mt-3 list-disc pl-5">
          {avail.map(a => <li key={a.id}>Day {a.dayOfWeek}: {a.startTime} - {a.endTime}</li>)}
        </ul>
      </div>
    </div>
  )
}
