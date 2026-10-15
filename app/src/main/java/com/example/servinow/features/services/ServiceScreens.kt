package com.example.servinow.features.services
import com.example.servinow.domain.model.Professional

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

val servicePages=setOf("offers","confirm-offer","tracking","finish","review","worker","offer","my-offers","profile","cv-preview","service-chat")
fun serviceTitle(page:String)=when(page) {
    "offers"->"Ofertas recibidas";"confirm-offer"->"Elegir trabajador";"tracking"->"Seguimiento del trabajo"
    "finish"->"Finalizar trabajo";"review"->"Calificar al trabajador";"worker"->"Panel del trabajador"
    "offer"->"Enviar oferta";"my-offers"->"Mis ofertas";"profile"->"Perfil del trabajador";"cv-preview"->"Experiencia profesional";else->"Conversación"
}
private fun money(value:Long)="$${NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(value)} COP"
@Composable
fun ServiceContent(page:String,vm:ServiceViewModel,go:(String)->Unit,openJob:(Int)->Unit) {
    val professionals=vm.professionals
    val s by vm.state.collectAsState();val data=s.data
    val selected=data.offers.find { it.id==s.selected }
    val assigned=data.offers.find { it.id==data.accepted[s.job] }
    val worker=professionals.firstOrNull { it.id==(assigned?.worker ?: selected?.worker ?: s.profile) } ?: professionals.first()
    s.error?.let { text -> AlertDialog(onDismissRequest={vm.error(null)},title={Text("ServiNow")},text={Text(text)},confirmButton={TextButton(onClick={vm.error(null)}){Text("Entendido")}}) }
    Text("Modo local · personas y ofertas de ejemplo",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    when(page) {
        "offers" -> {
            Title("Elige con confianza.");Text(s.title)
            val offers=data.offers.filter { it.job==s.job }
            Text("${offers.size} ofertas recibidas")
            if(offers.isEmpty()) Text("Aún no hay ofertas para este trabajo.")
            offers.forEach { offer ->
                val person=professionals.first { it.id==offer.worker }
                CardBlock {
                    Person(person);Text(offer.message);Text(money(offer.price),fontWeight=FontWeight.Bold)
                    Text("${offer.availability} · ${offer.duration}")
                    TextButton(onClick={vm.profile(person.id);go("profile")}){Text("Ver perfil")}
                    if(s.job !in data.accepted && s.job !in data.completed) Action("Elegir a ${person.name.substringBefore(' ')}") {vm.select(offer);go("confirm-offer")}
                    else Text(if(data.accepted[s.job]==offer.id) "Oferta aceptada" else "No seleccionada")
                }
            }
            if(s.job in data.accepted) Action("Ver seguimiento"){go("tracking")}
        }
        "confirm-offer" -> {
            Title("Un paso más.");Text("Confirma al trabajador para comenzar a coordinar el servicio.")
            if(selected!=null) {
                CardBlock { Person(professionals.first {it.id==selected.worker});Text(selected.title,fontWeight=FontWeight.Bold);Text(money(selected.price));Text("${selected.availability} · ${selected.duration}");Text(selected.message) }
                Text("La asignación se guarda en este dispositivo. El trabajo dejará de aceptar ofertas aquí. No se enviará una notificación remota.")
                Action("Confirmar trabajador") {if(vm.accept()) go("tracking")}
            } else Text("Selecciona primero una oferta.")
            Secondary("Seguir comparando"){go("offers")}
        }
        "tracking" -> {
            Title("Un trabajo en buenas manos.")
            if(assigned==null) {Text("Este trabajo todavía no tiene un trabajador asignado.");Action("Ver ofertas"){go("offers")}}
            else {
                Text(if(s.job in data.completed) "Finalizado" else "En progreso",color=MaterialTheme.colorScheme.primary)
                Text(s.title,fontWeight=FontWeight.Bold)
                CardBlock {Person(worker);TextButton(onClick={vm.profile(worker.id);go("profile")}){Text("Ver perfil y CV")}}
                listOf("Oferta aceptada" to "${worker.name} · ${money(assigned.price)}","Trabajo en progreso" to if(s.job in data.doneRequests) "El trabajador indicó que terminó. Falta tu confirmación." else "Coordina los avances del servicio.","Finalización y reseña" to if(s.job in data.completed) "Trabajo finalizado por el solicitante." else "Confirma cuando el trabajo termine.").forEach { (title,body)->CardBlock {Text(title,fontWeight=FontWeight.Bold);Text(body)} }
                if(s.job !in data.completed) Action("Marcar como finalizado"){go("finish")}
                else if(data.reviews.none {it.job==s.job}) Action("Calificar al trabajador"){go("review")}
                else Text("Reseña publicada en este dispositivo.")
                Secondary("Ver conversación"){val id=s.job.removePrefix("job-").toIntOrNull();if(id!=null)openJob(id) else go("service-chat")}
            }
        }
        "finish" -> {
            Title("¿Todo quedó listo?");Text("Confirma que el trabajo terminó. Dejará de recibir ofertas.")
            CardBlock {Text(s.title,fontWeight=FontWeight.Bold);Person(worker)}
            Action("Sí, finalizar trabajo"){if(vm.finish())go("review")}
            Secondary("Volver al seguimiento"){go("tracking")}
        }
        "review" -> {
            Title("Tu experiencia ayuda.");CardBlock {Person(worker)}
            if(s.job !in data.completed) Text("Las reseñas se habilitan al finalizar un trabajo con esta persona.")
            else if(data.reviews.any {it.job==s.job}) Text("Ya publicaste una reseña para este trabajo.")
            else {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) { (1..5).forEach { value ->
                    TextButton(onClick={vm.rating(value)},modifier=Modifier.weight(1f).semantics {contentDescription="$value de 5 estrellas"},contentPadding=PaddingValues(0.dp)) {Text(if(value<=s.stars) "★" else "☆",style=MaterialTheme.typography.headlineMedium)}
                } }
                Text("${s.stars} de 5")
                Input("Comentario sobre el trabajador",s.review,true){vm.review(it)}
                Action("Publicar reseña"){if(vm.publishReview()){vm.profile(worker.id);go("profile")}}
            }
            Secondary("Ahora no"){go("tracking")}
        }
        "worker" -> {
            Title("Hola, Andrés.\nTu talento hace falta.");Action("Buscar trabajos"){go("home")}
            val mine=data.offers.filter {it.worker=="andres" && (it.mine || data.accepted[it.job]==it.id)}
            val active=mine.filter {data.accepted[it.job]==it.id && it.job !in data.completed}
            CardBlock {
                Text("${active.size} trabajos activos")
                Text("${mine.count {it.mine && it.job !in data.accepted}} ofertas pendientes")
                Text("${mine.count {data.accepted[it.job]==it.id && it.job in data.completed}} trabajos finalizados")
                val ratings=data.reviews.filter {it.worker=="andres"};Text(if(ratings.isEmpty()) "Sin calificaciones locales" else "Calificación: ${String.format(Locale.US,"%.1f",ratings.map {it.stars}.average())} ★")
            }
            if(active.isEmpty()) Text("No tienes trabajos activos en este dispositivo.")
            active.forEach { p -> CardBlock {Text(p.title,fontWeight=FontWeight.Bold);Action("Ver detalles"){vm.select(p);go("tracking")};Secondary("Avisar que terminé"){vm.select(p);vm.workerDone()}} }
            Action("Ver mis ofertas"){go("my-offers")}
            Secondary("Mi perfil profesional"){vm.profile("andres");go("profile")}
        }
        "offer" -> {
            Title("Ofrece tu experiencia.");Text(s.title)
            Input("Tu precio (COP)",s.price,numeric=true){vm.form(price=it)}
            Input("¿Qué incluye tu oferta?",s.message,true){vm.form(message=it)}
            Input("Disponibilidad",s.availability){vm.form(availability=it)}
            Input("Duración estimada",s.duration){vm.form(duration=it)}
            Action("Enviar oferta"){if(vm.send())go("my-offers")}
            Text("Se guarda localmente como oferta de Andrés. No se envía a otra persona.")
        }
        "my-offers" -> {
            Title("Mis ofertas");Text("Cada propuesta puede ser una nueva oportunidad.")
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Todas","Pendientes","Aceptadas").forEach {f->FilterChip(s.filter==f,{vm.filter(f)},label={Text(f)})}}
            val offers=data.offers.filter {it.mine || (it.worker=="andres" && data.accepted[it.job]==it.id)}.filter {when(s.filter){"Pendientes"->it.job !in data.accepted;"Aceptadas"->data.accepted[it.job]==it.id;else->true}}
            if(offers.isEmpty()) Text("No tienes ofertas en esta sección.")
            offers.forEach { p -> CardBlock {
                Text(p.title,fontWeight=FontWeight.Bold);Text(money(p.price));Text(p.availability)
                Text(when {data.accepted[p.job]==p.id -> if(p.job in data.completed) "Finalizada" else "Aceptada";p.job in data.accepted->"No seleccionada";else->"Pendiente de respuesta"})
                if(data.accepted[p.job]==p.id) Action("Ver trabajo"){vm.select(p);go("tracking")}
                else TextButton(onClick={p.job.removePrefix("job-").toIntOrNull()?.let(openJob)}){Text("Ver publicación")}
            } }
        }
        "profile","cv-preview" -> {
            val person=professionals.first {it.id==s.profile}
            CardBlock {Person(person);Text("${person.years} años de experiencia · Bogotá");Text("Perfil de demostración")}
            if(page=="cv-preview") {
                Title("Experiencia profesional");Text("${person.trade}. Atención a hogares, preparación del área y limpieza al terminar.")
                Text("Resumen de ejemplo. Todavía no hay un archivo de CV adjunto ni verificación de esta experiencia.")
                Secondary("Volver al perfil"){go("profile")}
            } else {
                Title("Hola, soy ${person.name.substringBefore(' ')}.");Text("Me gusta explicar cada arreglo y dejar todo limpio al terminar. Atiendo en Chapinero y Teusaquillo.")
                Secondary("Ver currículum"){go("cv-preview")}
                Title("Lo que dice la comunidad")
                val reviews=data.reviews.filter {it.worker==person.id}
                if(reviews.isEmpty()) Text("Aún no hay reseñas locales.")
                reviews.forEach { r -> CardBlock {Text("Tú · ${r.stars} de 5",fontWeight=FontWeight.Bold);Text("★".repeat(r.stars),color=MaterialTheme.colorScheme.primary);Text(r.text)} }
                val eligible=data.offers.firstOrNull {it.worker==person.id && data.accepted[it.job]==it.id && it.job in data.completed && data.reviews.none {r->r.job==it.job}}
                if(eligible!=null) Action("Calificar y comentar"){vm.select(eligible);go("review")}
                else Text("Podrás calificar después de finalizar un trabajo con esta persona.")
            }
        }
        "service-chat" -> {Title("Conversación del ejemplo");Text("Este caso de pintura sirve para probar la selección y finalización. Para comentar una publicación del feed, abre su detalle y pulsa Abrir conversación.");Secondary("Volver al seguimiento"){go("tracking")}}
    }
}
@Composable private fun Title(text:String){Text(text,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
@Composable private fun Person(p:Professional){Text(p.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text(p.trade)}
@Composable private fun CardBlock(content:@Composable ColumnScope.()->Unit){Surface(shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)}}
@Composable private fun Action(text:String,click:()->Unit){Button(onClick=click,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().heightIn(min=50.dp)){Text(text)}}
@Composable private fun Secondary(text:String,click:()->Unit){OutlinedButton(onClick=click,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(text)}}
@Composable private fun Input(label:String,value:String,multi:Boolean=false,numeric:Boolean=false,change:(String)->Unit){OutlinedTextField(value,change,label={Text(label)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),singleLine=!multi,minLines=if(multi)3 else 1,maxLines=if(multi)6 else 1,keyboardOptions=KeyboardOptions(keyboardType=if(numeric)KeyboardType.Number else KeyboardType.Text))}
