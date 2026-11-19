const {chromium}=require('playwright');const fs=require('fs');const path=require('path');
(async()=>{
 const [,,outDir,t0s,t1s,step]=process.argv; const dir=__dirname;
 const b=await chromium.launch();const p=await b.newPage({viewport:{width:1920,height:1080}});
 await p.goto('file://'+path.join(dir,'index.html'));await p.evaluate(()=>document.fonts.ready);
 const has={};for(const s of ['shot1','shot2','shot3','shot4'])has[s]=fs.existsSync(path.join(dir,'frames',s,'0001.png'));
 await p.evaluate(h=>window.HAS3D=h,has);
 fs.mkdirSync(outDir,{recursive:true});
 const t0=+t0s,t1=+t1s,st=+(step||1);
 for(let f=Math.round(t0*30);f<Math.round(t1*30);f+=st){
   await p.evaluate(t=>window.renderAt(t),f/30);await p.evaluate(()=>window.waitImg());
   await p.screenshot({path:path.join(outDir,String(f).padStart(4,'0')+'.jpg'),type:'jpeg',quality:94});
 }
 await b.close();
})();
