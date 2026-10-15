package com.example.servinow.domain.validation
import com.example.servinow.domain.model.*
fun reputationLevel(points:Int):String=when { points>=1500->"Referente";points>=500->"Experto";points>=100->"Colaborador";else->"Vecino" }
fun professionalError(a:AccountData):String?=when { a.title.isBlank()||a.about.isBlank()->"Completa el título y la presentación.";a.years.toIntOrNull() !in 0..80->"Escribe una experiencia entre 0 y 80 años.";else->null }
