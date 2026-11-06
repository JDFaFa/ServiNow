import {access} from '../src/notifications.js';
// Explicit test fixtures. Create-only writes preserve all existing documents.
export const jobs=[
 ['Plomería','Reparar fuga en lavamanos',85000],['Electricidad','Cambiar tres tomacorrientes',110000],
 ['Carpintería','Ajustar puerta de madera',140000],['Mantenimiento','Instalar dos repisas',75000],
 ['Pintura','Pintar una habitación',280000],['Limpieza','Limpieza de apartamento',120000],
 ['Mecánica','Revisión preventiva de bicicleta',65000],['Jardinería','Podar jardín pequeño',95000],
 ['Climatización','Mantenimiento de aire acondicionado',180000],['Cerrajería','Cambiar cerradura principal',130000],
 ['Mudanzas','Trasladar muebles dentro de Armenia',350000],['Plomería','Instalar grifería de cocina',100000],
 ['Electricidad','Instalar lámpara de techo',90000],['Carpintería','Reparar cajón de armario',80000],
 ['Pintura','Pintar pared de sala',160000],['Limpieza','Limpiar ventanas de vivienda',70000],
 ['Jardinería','Organizar plantas de balcón',60000],['Mantenimiento','Sellar juntas del baño',115000],
 ['Cerrajería','Revisar cerradura de habitación',85000],['Mudanzas','Mover escritorio y estantería',150000]
];
function encode(v){if(v===null)return {nullValue:null};if(typeof v==='number')return Number.isInteger(v)?{integerValue:String(v)}:{doubleValue:v};return {stringValue:v};}
export async function seed(env){
 const token=await access(env),base=`https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents`;
 const headers={Authorization:`Bearer ${token}`,'Content-Type':'application/json'};let created=0,existing=0;
 async function create(collection,id,data){const fields=Object.fromEntries(Object.entries(data).map(([k,v])=>[k,encode(v)]));if(collection==='posts'){fields.createdAt={timestampValue:new Date().toISOString()};fields.updatedAt=fields.createdAt;}
 const r=await fetch(`${base}/${collection}?documentId=${id}`,{method:'POST',headers,body:JSON.stringify({fields}),signal:AbortSignal.timeout(15000)});
 if(r.status===409){existing++;return;}if(!r.ok)throw Error(`Seed failed: ${r.status}`);created++;}
 for(let i=1;i<=4;i++)await create('profiles',`demo_20261104_client_${i}`,{name:`Cliente ficticio ${i} · PRUEBA`,city:'Armenia',profession:'',bio:'Perfil ficticio para pruebas de ServiNow. No corresponde a una persona real.',experience:'',training:'',skills:'',years:0,photoUri:null});
 for(let i=0;i<jobs.length;i++){const [category,title,budget]=jobs[i];await create('posts',`demo_20261104_job_${String(i+1).padStart(2,'0')}`,{author:`demo_20261104_client_${i%4+1}`,title:`[PRUEBA] ${title}`,description:`Publicación ficticia para pruebas. Se necesita ${title.toLowerCase()} en Armenia, Quindío. Permite probar filtros, detalle y postulaciones; no representa una contratación real. La ubicación es aproximada y no corresponde a un domicilio.`,category,city:'Armenia',latitude:4.535+(i%5)*0.003,longitude:-75.681+Math.floor(i/5)*0.003,budget,imageUri:null,status:i<16?'VERIFIED':'PENDING',assigned:null,reason:''});}
 let verified=0;for(let i=0;i<jobs.length;i++){const r=await fetch(`${base}/posts/demo_20261104_job_${String(i+1).padStart(2,'0')}`,{headers});if(r.ok)verified++;}
 return {created,existing,verified,available:16,pending:4};
}
