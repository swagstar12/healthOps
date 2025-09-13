import React, { useEffect, useState } from 'react'
import { api } from '../api'

type Doctor = { id:number, user:{ email:string, fullName:string}, specialization?:string, phone?:string }

export default function AdminDashboard(){
  const [doctors, setDoctors] = useState<Doctor[]>([])
  const [form, setForm] = useState({email:'', fullName:'', password:'', specialization:'', phone:''})

  async function load(){ const {data} = await api.get('/admin/doctors'); setDoctors(data) }
  useEffect(()=>{ load() }, [])

  async function addDoctor(e: React.FormEvent){
    e.preventDefault()
    await api.post('/admin/doctors', form)
    setForm({email:'', fullName:'', password:'', specialization:'', phone:''})
    load()
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Admin Dashboard</h1>
      <div className="card">
        <h2 className="font-semibold mb-2">Add Doctor</h2>
        <form className="grid grid-cols-2 gap-3" onSubmit={addDoctor}>
          <input className="input" placeholder="Email" value={form.email} onChange={e=>setForm({...form, email:e.target.value})} />
          <input className="input" placeholder="Full Name" value={form.fullName} onChange={e=>setForm({...form, fullName:e.target.value})} />
          <input className="input" placeholder="Password" type="password" value={form.password} onChange={e=>setForm({...form, password:e.target.value})} />
          <input className="input" placeholder="Specialization" value={form.specialization} onChange={e=>setForm({...form, specialization:e.target.value})} />
          <input className="input" placeholder="Phone" value={form.phone} onChange={e=>setForm({...form, phone:e.target.value})} />
          <button className="btn col-span-2">Create</button>
        </form>
      </div>

      <div className="card">
        <h2 className="font-semibold mb-2">Doctors</h2>
        <table className="w-full text-left">
          <thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Specialization</th><th>Phone</th></tr></thead>
          <tbody>
            {doctors.map(d => (
              <tr key={d.id}>
                <td>{d.id}</td>
                <td>{d.user?.fullName}</td>
                <td>{d.user?.email}</td>
                <td>{d.specialization}</td>
                <td>{d.phone}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
