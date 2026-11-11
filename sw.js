const CACHE='break-tracker-v6-clear-glass';const FILES=['./','./index.html','./kitt.html','./manifest.webmanifest','./icon.svg'];self.addEventListener('install',e=>{e.waitUntil(caches.open(CACHE).then(c=>c.addAll(FILES)).then(()=>self.skipWaiting()))});self.addEventListener('activate',e=>{e.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim()))});self.addEventListener('fetch',e=>{if(e.request.method!=='GET'||new URL(e.request.url).origin!==self.location.origin)return;e.respondWith(fetch(e.request).then(r=>{if(r.ok){const copy=r.clone();caches.open(CACHE).then(c=>c.put(e.request,copy))}return r}).catch(()=>caches.match(e.request)))});self.addEventListener('notificationclick',event=>{
  event.notification.close();
  const path=event.notification.data?.url||'./index.html';
  const target=new URL(path,self.registration.scope);
  // Never navigate to a URL outside the installed tracker origin.
  if(target.origin!==self.location.origin)return;
  event.waitUntil((async()=>{
    const windows=await clients.matchAll({type:'window',includeUncontrolled:true});
    const existing=windows.find(client=>new URL(client.url).pathname===target.pathname);
    if(existing){await existing.focus();return}
    await clients.openWindow(target.href);
  })());
});
