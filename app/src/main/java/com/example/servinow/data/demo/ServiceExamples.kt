package com.example.servinow.data.demo
import com.example.servinow.domain.model.*
val professionals = listOf(Professional("andres", "Andrés Moreno", "Plomería y mantenimiento", 6),
    Professional("laura", "Laura Pérez", "Pintura y acabados", 5), Professional("camilo", "Camilo Díaz", "Pintura y mantenimiento", 4))
val demoOffers = listOf(
    Proposal("demo-a","demo-paint","Pintar una habitación","andres",110000,"Incluye mano de obra y protección del área.","Mañana","1 día"),
    Proposal("demo-l","demo-paint","Pintar una habitación","laura",125000,"Dos manos de pintura y limpieza al terminar.","Viernes","1 día"),
    Proposal("demo-c","demo-paint","Pintar una habitación","camilo",120000,"Visita y preparación de superficies incluidas.","Sábado por la mañana","1 día"))
