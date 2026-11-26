
/* Offline vector frames and arena backgrounds. Existing photo geometry is retained. */
const arenaBackgrounds={
  arenaBlue:{name:'Electric Blue',colors:['#04091d','#1354ac','#27c9ff'],preview:'linear-gradient(145deg,#04091d,#1354ac,#27c9ff)'},
  arenaGold:{name:'Crimson Gold',colors:['#200509','#a31932','#ffcb57'],preview:'linear-gradient(145deg,#200509,#a31932,#ffcb57)'}
};
const arenaFrames={
  tagda2:{name:'Tagda #2: Royal Clash',metal:['#392000','#ffce56','#fff6c6','#a65b10'],accent:'#ffd45f',left:'#ca2438',right:'#175bc5',dark:'#241323',light:'#fff1b0'},
  tagda3:{name:'Tagda #3: Neon Battle',metal:['#172450','#40cfff','#f0faff','#a155ff'],accent:'#69efff',left:'#c12887',right:'#008c83',dark:'#0b1032',light:'#e4fcff'}
};
function arenaGradient(c,x0,y0,x1,y1,colors){
  const g=c.createLinearGradient(x0,y0,x1,y1);
  colors.forEach((v,i)=>g.addColorStop(i/(colors.length-1),v));return g;
}
function arenaPolygon(c,points,fill,stroke,width){
  c.beginPath();points.forEach((p,i)=>i?c.lineTo(p[0],p[1]):c.moveTo(p[0],p[1]));c.closePath();
  if(fill){c.fillStyle=fill;c.fill();}if(stroke){c.strokeStyle=stroke;c.lineWidth=width||1;c.stroke();}
}
function arenaSpark(c,x,y,r,color){
  c.save();c.shadowColor=color;c.shadowBlur=r*1.5;
  arenaPolygon(c,[[x,y-r],[x+r*.18,y-r*.18],[x+r,y],[x+r*.18,y+r*.18],[x,y+r],[x-r*.18,y+r*.18],[x-r,y],[x-r*.18,y-r*.18]],color);c.restore();
}
function paintArenaBackground(c,key){
  const p=arenaBackgrounds[key];c.save();
  if(!p){c.fillStyle=key;c.fillRect(0,0,1200,900);c.restore();return;}
  c.fillStyle=arenaGradient(c,0,0,1200,900,[p.colors[0],p.colors[1],p.colors[0]]);c.fillRect(0,0,1200,900);
  const glow=c.createRadialGradient(600,410,10,600,410,720);glow.addColorStop(0,p.colors[2]);glow.addColorStop(.32,p.colors[1]);glow.addColorStop(1,p.colors[0]);
  c.globalAlpha=.63;c.fillStyle=glow;c.fillRect(0,0,1200,900);
  c.globalAlpha=.17;
  for(let i=0;i<13;i++){const x=i*115-160;arenaPolygon(c,[[x,0],[x+30,0],[x+350,900],[x+272,900]],p.colors[2]);}
  c.globalAlpha=.25;c.strokeStyle=p.colors[2];c.lineWidth=2;
  for(let i=0;i<6;i++){c.beginPath();c.ellipse(600,840,125+i*130,36+i*27,0,0,Math.PI*2);c.stroke();}
  c.globalAlpha=.7;
  for(let i=0;i<44;i++){const x=(i*197+37)%1200,y=(i*137+53)%900;arenaSpark(c,x,y,2+(i%4),p.colors[2]);}
  c.restore();
}
function paintArenaFrame(c,key){
  const p=arenaFrames[key];if(!p)return;c.save();c.lineJoin='round';
  const metal=arenaGradient(c,0,0,980,880,p.metal);
  const top=arenaGradient(c,0,0,0,750,[p.light,p.accent,p.metal[0],p.light]);
  for(const x of [70,630]){
    const w=500,y=70,h=680,k=38;
    const shape=[[x-k/2,y+17],[x+17,y-k/2],[x+w-17,y-k/2],[x+w+k/2,y+17],[x+w+k/2,y+h-17],[x+w-17,y+h+k/2],[x+17,y+h+k/2],[x-k/2,y+h-17]];
    c.shadowColor=p.accent;c.shadowBlur=20;arenaPolygon(c,shape,null,p.dark,32);
    c.shadowBlur=0;arenaPolygon(c,shape,null,metal,20);arenaPolygon(c,shape,null,p.light,3);
    for(const yy of [y+17,y+h-17]){arenaSpark(c,x-k/2,yy,13,p.light);arenaSpark(c,x+w+k/2,yy,13,p.light);}
  }
  arenaPolygon(c,[[600,22],[612,44],[607,750],[600,775],[593,750],[588,44]],top,p.dark,3);
  for(const side of [0,1]){
    c.save();if(side){c.translate(1200,0);c.scale(-1,1);}
    for(let i=0;i<3;i++){
      const yy=765+i*23;
      arenaPolygon(c,[[18,yy],[245,yy+14],[366,yy+34],[196,yy+39],[52,yy+22]],metal,p.dark,3);
    }
    c.restore();
  }
  const shield=[[340,761],[435,745],[510,745],[600,710],[690,745],[765,745],[860,761],[814,857],[600,896],[386,857]];
  c.shadowColor=p.accent;c.shadowBlur=17;arenaPolygon(c,shield,p.dark,metal,9);c.shadowBlur=0;
  arenaPolygon(c,[[362,775],[450,758],[600,743],[750,758],[838,775],[794,846],[600,878],[406,846]],arenaGradient(c,0,748,0,890,[p.dark,p.metal[0],p.dark]),p.accent,2);
  if(key==='tagda2'){
    arenaPolygon(c,[[548,748],[540,704],[572,727],[600,682],[628,727],[660,704],[652,748]],metal,p.dark,3);
    arenaSpark(c,600,692,10,p.light);
  }else{
    arenaPolygon(c,[[610,680],[566,733],[600,728],[590,758],[637,704],[605,712]],metal,p.dark,3);
    arenaSpark(c,632,694,10,p.light);
  }
  c.textAlign='center';c.textBaseline='middle';c.lineJoin='round';
  c.font='900 70px Arial';c.lineWidth=10;c.strokeStyle=p.dark;c.strokeText('TAGDA',600,792);
  c.fillStyle=arenaGradient(c,0,756,0,819,[p.light,p.accent,p.metal[1]]);c.fillText('TAGDA',600,792);
  c.font='900 46px Arial';c.lineWidth=7;c.strokeText('MUQABLA',600,844);c.fillStyle='#ffffff';c.fillText('MUQABLA',600,844);
  for(const [x,label,color] of [[220,'A',p.left],[980,'B',p.right]]){
    c.shadowColor=p.accent;c.shadowBlur=12;c.beginPath();c.ellipse(x,815,78,55,0,0,Math.PI*2);c.fillStyle=arenaGradient(c,x-70,760,x+70,870,[p.dark,color,p.dark]);c.fill();c.shadowBlur=0;c.lineWidth=9;c.strokeStyle=metal;c.stroke();
    c.beginPath();c.ellipse(x,815,65,43,0,0,Math.PI*2);c.lineWidth=2;c.strokeStyle=p.light;c.stroke();
    c.font='900 68px Arial';c.lineWidth=6;c.strokeStyle=p.dark;c.strokeText(label,x,817);c.fillStyle='#ffffff';c.fillText(label,x,817);
  }
  for(const [x,y] of [[24,24],[1176,24],[24,875],[1176,875],[600,38]])arenaSpark(c,x,y,17,p.light);
  c.restore();
}
function addArenaBackgroundButtons(toolbar){
  const row=toolbar.querySelector('.bgColorScroll');if(!row)return;
  let i=0;
  for(const [key,p] of Object.entries(arenaBackgrounds)){
    const button=document.createElement('button');button.type='button';button.className='tool bgSwatch';button.style.background=p.preview;
    button.style.color='#fff';button.style.textShadow='0 1px 4px #000';button.style.fontWeight='700';button.style.fontSize='12px';
    button.textContent=p.name;button.title=p.name;button.setAttribute('aria-label',p.name+' background');
    button.addEventListener('click',()=>{S.bg=key;draw();});row.insertBefore(button,row.children[i++]||null);
  }
}
function addArenaFrameButtons(toolbar){
  for(const [key,p] of Object.entries(arenaFrames)){
    const button=document.createElement('button');button.type='button';button.className='tool';button.title=p.name;button.setAttribute('aria-label',p.name);
    button.style.flex='0 0 132px';button.style.width='132px';button.style.minWidth='132px';button.style.maxWidth='132px';
    const thumb=document.createElement('canvas');thumb.width=120;thumb.height=90;thumb.style.width='88px';thumb.style.height='66px';
    const tc=thumb.getContext('2d');tc.scale(.1,.1);paintArenaBackground(tc,key==='tagda2'?'arenaGold':'arenaBlue');paintArenaFrame(tc,key);
    const label=document.createElement('span');label.textContent=p.name;label.style.fontSize='11px';
    button.append(thumb,label);button.addEventListener('click',()=>{S.frameStyle=key;S.decorFrame=true;draw();});toolbar.appendChild(button);
  }
}
