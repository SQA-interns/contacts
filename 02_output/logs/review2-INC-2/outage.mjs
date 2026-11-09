const EM='http://localhost:9099',P='demo-contact-groups';
const email=`out.${Date.now().toString(36)}@acme.com`;
const em=async(m,p,b,a)=>{const r=await fetch(EM+p,{method:m,headers:{'Content-Type':'application/json',...(a?{Authorization:'Bearer owner'}:{})},body:b&&JSON.stringify(b)});return r.json()};
await em('POST',`/identitytoolkit.googleapis.com/v1/projects/${P}/accounts`,{email,password:'Geslo1234',displayName:'Out Age',emailVerified:true},true);
const t=await em('POST','/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key',{email,password:'Geslo1234',returnSecureToken:true});
console.log(t.idToken);
