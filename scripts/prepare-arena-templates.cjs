'use strict';
// Apply a small, idempotent integration to the existing embedded-image HTML before Capacitor copies it.
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
function patch(html){
  const marker='<script src="arena-templates.js"></script>';
  if(html.includes(marker)){
    for(const required of ['paintArenaBackground(ctx,S.bg);','if(S.decorFrame && arenaFrames[S.frameStyle])paintArenaFrame(ctx,S.frameStyle);','addArenaBackgroundButtons(t);','addArenaFrameButtons(t);'])if(!html.includes(required))throw Error('Partial arena patch: '+required);
    return html;
  }
  function once(old,replacement){if(html.split(old).length!==2)throw Error('Arena integration anchor not unique: '+old.slice(0,90));html=html.replace(old,replacement);}
  once('<script>\nconst cv=',marker+'\n<script>\nconst cv=');
  once('ctx.clearRect(0,0,1200,900);ctx.fillStyle=S.bg;ctx.fillRect(0,0,1200,900);','ctx.clearRect(0,0,1200,900);paintArenaBackground(ctx,S.bg);');
  const line="if(S.decorFrame && S.frameStyle==='tagda1' && tagdaFrame1.complete && tagdaFrame1.naturalWidth){ctx.drawImage(tagdaFrame1,0,0,1200,900)}";
  once(line,line+'\nif(S.decorFrame && arenaFrames[S.frameStyle])paintArenaFrame(ctx,S.frameStyle);');
  once('if(!window.bgTapListenerAdded){','addArenaBackgroundButtons(t);\nif(!window.bgTapListenerAdded){');
  const thumb="if(c1&&tagdaFrame1.complete&&tagdaFrame1.naturalWidth)c1.getContext('2d').drawImage(tagdaFrame1,0,0,112,92);";
  once(thumb,thumb+'\naddArenaFrameButtons(t);');
  return html;
}
const web=path.join(__dirname,'..','www');
const file=path.join(web,'index.html');
const old=fs.readFileSync(file,'utf8');
const next=patch(old);
new vm.Script(fs.readFileSync(path.join(web,'arena-templates.js'),'utf8'));
new vm.Script(next.match(/<script>([\s\S]*?)<\/script>/)[1]);
if(next!==old)fs.writeFileSync(file,next,'utf8');
console.log('Arena templates ready: 2 frames and 2 backgrounds.');
