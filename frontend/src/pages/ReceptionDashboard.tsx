import React, { useEffect, useState } from 'react'
import { api } from '../api'

type Patient = { id:number, code:string, fullName:string, phone?:string }
type Doctor = { id:number, user:{ fullName:string } }

export default function ReceptionDashboard(){
  const [patients, setPatients] = useState<Patient[]>([])
  const [doctors, setDoctors] = useState<Doctor[]>([])
  const [pForm, setPForm] = useState({ code:'P-1001', fullName:'John Doe', dob:'', phone:'', address:'' })
  const [aForm, setAForm] = useState({ patientId:0, doctorId:0, scheduledAt:'', reason:'' })
  const [appointments, setAppointments] = useState<any[]>([])

  async function load(){
    const ps = await api.get('/reception/patients'); setPatients(ps.data)
    const ds = await api.get('/admin/doctors'); setDoctors(ds.data)
    const as = await api.get('/reception/appointments'); setAppointments(as.data)
  }
  useEffect(()=>{ load() }, [])

  async function addPatient(e: React.FormEvent){
    e.preventDefault()
    await api.post('/reception/patients', pForm)
    setPForm({ code:'', fullName:'', dob:'', phone:'', address:'' })
    load()
  }

  async function addAppointment(e: React.FormEvent){
    e.preventDefault()
    await api.post('/reception/appointments', aForm)
    setAForm({ patientId:0, doctorId:0, scheduledAt:'', reason:'' })
    load()
  }

  function downloadReport(pid:number){
    window.location.href = `/api/reports/patient/${pid}/visits.csv`
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Receptionist Dashboard</h1>

      <div className="card">
        <h2 className="font-semibold mb-2">Add Patient</h2>
        <form className="grid grid-cols-2 gap-3" onSubmit={addPatient}>
          <input className="input" placeholder="Code" value={pForm.code} onChange={e=>setPForm({...pForm, code:e.target.value})} />
          <input className="input" placeholder="Full Name" value={pForm.fullName} onChange={e=>setPForm({...pForm, fullName:e.target.value})} />
          <input className="input" placeholder="DOB (YYYY-MM-DD)" value={pForm.dob} onChange={e=>setPForm({...pForm, dob:e.target.value})} />
          <input className="input" placeholder="Phone" value={pForm.phone} onChange={e=>setPForm({...pForm, phone:e.target.value})} />
          <input className="input col-span-2" placeholder="Address" value={pForm.address} onChange={e=>setPForm({...pForm, address:e.target.value})} />
          <button className="btn col-span-2">Create</button>
        </form>
      </div>

      <div className="grid grid-cols-2 gap-6">
        <div className="card">
          <h2 className="font-semibold mb-2">Patients</h2>
          <ul className="list-disc pl-5">
            {patients.map(p => (
              <li key={p.id} className="flex justify-between">
                <span>{p.code} — {p.fullName}</span>
                <button className="btn" onClick={()=>downloadReport(p.id)}>Download visits CSV</button>
              </li>
            ))}
          </ul>
        </div>
        <div className="card">
          <h2 className="font-semibold mb-2">Create Appointment</h2>
          <form className="space-y-3" onSubmit={addAppointment}>
            <select className="input" value={aForm.patientId} onChange={e=>setAForm({...aForm, patientId:Number(e.target.value)})}>
              <option value={0}>Select patient</option>
              {patients.map(p => <option key={p.id} value={p.id}>{p.fullName}</option>)}
            </select>
            <select className="input" value={aForm.doctorId} onChange={e=>setAForm({...aForm, doctorId:Number(e.target.value)})}>
              <option value={0}>Select doctor</option>
              {doctors.map(d => <option key={d.id} value={d.id}>{d.user?.fullName}</option>)}
            </select>
            <input className="input" placeholder="Scheduled At (ISO e.g. 2025-08-29T10:00:00Z)" value={aForm.scheduledAt} onChange={e=>setAForm({...aForm, scheduledAt:e.target.value})} />
            <textarea className="input" placeholder="Reason" value={aForm.reason} onChange={e=>setAForm({...aForm, reason:e.target.value})} />
            <button className="btn w-full">Schedule</button>
          </form>
        </div>
      </div>

      <div className="card">
        <h2 className="font-semibold mb-2">Appointments</h2>
        <table className="w-full text-left">
          <thead><tr><th>ID</th><th>Patient</th><th>Doctor</th><th>Time</th><th>Status</th></tr></thead>
          <tbody>
            {appointments.map(a => (
              <tr key={a.id}>
                <td>{a.id}</td>
                <td>{a.patient?.fullName}</td>
                <td>{a.doctor?.user?.fullName}</td>
                <td>{a.scheduledAt}</td>
                <td>{a.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
