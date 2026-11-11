export class ApiError extends Error { constructor(status,code){super(code);this.status=status;} }
export function descriptionInput(body){
 if(!body || typeof body.text!=='string' || body.text.trim().length<10 || body.text.length>3000) throw new ApiError(400,'INVALID_DESCRIPTION');
 if(!['es','en'].includes(body.language)) throw new ApiError(400,'INVALID_LANGUAGE');
 if(/[\w.+-]+@[\w.-]+\.[a-z]{2,}|(?:\+?\d[\s().-]*){9,}/i.test(body.text)) throw new ApiError(400,'REMOVE_CONTACT_DETAILS');
 if((body.text.match(/\p{L}+/gu)||[]).length<2)throw new ApiError(400,'AI_NEEDS_DETAILS');
 return {text:body.text.trim(),language:body.language};
}
export function imageType(bytes){
 if(!bytes.length || bytes.length>5*1024*1024) throw new ApiError(413,'IMAGE_SIZE');
 if(bytes[0]===255&&bytes[1]===216&&bytes[2]===255) return 'image/jpeg';
 if([137,80,78,71,13,10,26,10].every((v,i)=>bytes[i]===v))return 'image/png';
 if(new TextDecoder().decode(bytes.slice(0,4))==='RIFF'&&new TextDecoder().decode(bytes.slice(8,12))==='WEBP')return 'image/webp';
 throw new ApiError(415,'IMAGE_TYPE');
}
export async function boundedBody(request,max){
 if(Number(request.headers.get('content-length'))>max)throw new ApiError(413,'BODY_SIZE');
 const reader=request.body?.getReader();if(!reader)throw new ApiError(400,'EMPTY_BODY');
 let size=0;const chunks=[];
 try{while(true){const {value,done}=await reader.read();if(done)break;size+=value.length;if(size>max){await reader.cancel();throw new ApiError(413,'BODY_SIZE');}chunks.push(value);}}finally{reader.releaseLock();}
 const bytes=new Uint8Array(size);let offset=0;for(const part of chunks){bytes.set(part,offset);offset+=part.length;}return bytes;
}

// Never expose a model explanation as a replacement for a job description.
export function descriptionOutput(output){
 let value;try{
  if(typeof output!=='string')throw new Error();
  // Some providers wrap otherwise valid JSON in a Markdown code fence.
  // Accept only an entire fenced object, never arbitrary surrounding prose.
  const source=output.trim().replace(/^```(?:json)?\s*([\s\S]*?)\s*```$/i,'$1');
  value=JSON.parse(source);
 }catch{throw new ApiError(502,'AI_INVALID_RESPONSE');}
 if(value?.status==='needs_details')throw new ApiError(422,'AI_NEEDS_DETAILS');
 if(value?.status!=='ok'||typeof value.description!=='string'||value.description.trim().length<10||value.description.length>3000)throw new ApiError(502,'AI_INVALID_RESPONSE');
 const text=value.description.trim();
 if(/(?:no contiene una solicitud|proporcione una descripci[oó]n|no puedo|cannot rewrite|please provide|texto proporcionado|as an ai)/i.test(text))throw new ApiError(422,'AI_NEEDS_DETAILS');
 descriptionInput({text,language:'es'});
 return text;
}
