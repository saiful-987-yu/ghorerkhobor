const TZ='Asia/Dhaka';
const MSG_HEAD=['id','device_id','person','app_package','app_name','sender','title','text','conversation','posted_at','posted_time','image_id','saved_at'];
const DEV_HEAD=['device_id','person','role','registered_at','last_seen','last_upload','app_version'];

function props(){return PropertiesService.getScriptProperties();}
function ss(){return SpreadsheetApp.getActiveSpreadsheet();}
function now(){return Utilities.formatDate(new Date(),TZ,'yyyy-MM-dd HH:mm:ss');}
function out(o){return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON);}
function randomKey(){return Utilities.getUuid().replace(/-/g,'')+Utilities.getUuid().replace(/-/g,'');}

function makeSheet(name,head){
let s=ss().getSheetByName(name);
if(!s)s=ss().insertSheet(name);
if(s.getLastRow()===0){
s.getRange(1,1,1,head.length).setValues([head]);
s.setFrozenRows(1);
}
return s;
}

function setup(){
makeSheet('Messages',MSG_HEAD);
const dev=makeSheet('Devices',DEV_HEAD);
dev.getRange('A:G').setNumberFormat('@');
const p=props();
if(!p.getProperty('API_KEY'))p.setProperty('API_KEY',randomKey());
if(!p.getProperty('ADMIN_KEY'))p.setProperty('ADMIN_KEY',randomKey());
if(!p.getProperty('FOLDER_ID'))p.setProperty('FOLDER_ID',DriveApp.createFolder('GhorerKhobor_Images').getId());
showKeys();
}

function showKeys(){
const p=props();
Logger.log('API_KEY (user app): '+p.getProperty('API_KEY'));
Logger.log('ADMIN_KEY (admin app): '+p.getProperty('ADMIN_KEY'));
}

function doGet(){
return out({ok:true,service:'ghorer-khobor'});
}

function doPost(e){
let d;
try{d=JSON.parse(e.postData.contents);}catch(err){return out({ok:false,error:'bad_json'});}
const p=props();
const a=d.action;
const userActions=['register','upload'];
const adminActions=['devices','list','image'];
if(userActions.indexOf(a)>=0){
if(!d.key||d.key!==p.getProperty('API_KEY'))return out({ok:false,error:'unauthorized'});
}else if(adminActions.indexOf(a)>=0){
if(!d.adminKey||d.adminKey!==p.getProperty('ADMIN_KEY'))return out({ok:false,error:'unauthorized'});
}else{
return out({ok:false,error:'unknown_action'});
}
const lock=LockService.getScriptLock();
try{
lock.waitLock(30000);
if(a==='register')return out(register(d));
if(a==='upload')return out(upload(d));
if(a==='devices')return out(devices());
if(a==='list')return out(list(d));
return out(image(d));
}catch(err){
return out({ok:false,error:String(err)});
}finally{
try{lock.releaseLock();}catch(x){}
}
}

function findDevice(s,id){
const last=s.getLastRow();
if(last<2)return -1;
const v=s.getRange(2,1,last-1,1).getValues();
for(let i=0;i<v.length;i++){
if(String(v[i][0])===String(id))return i+2;
}
return -1;
}

function register(d){
if(!d.device_id)return {ok:false,error:'no_device_id'};
const s=ss().getSheetByName('Devices');
const row=findDevice(s,d.device_id);
const t=now();
if(row>0){
s.getRange(row,2,1,2).setValues([[d.person||'',d.role||'']]);
s.getRange(row,5).setValue(t);
s.getRange(row,7).setValue(d.app_version||'');
}else{
s.appendRow([String(d.device_id),d.person||'',d.role||'',t,t,'',d.app_version||'']);
}
return {ok:true};
}

function upload(d){
if(!d.device_id||!Array.isArray(d.items))return {ok:false,error:'bad_request'};
const dev=ss().getSheetByName('Devices');
const row=findDevice(dev,d.device_id);
if(row<0)return {ok:false,error:'not_registered'};
const person=dev.getRange(row,2).getValue();
const m=ss().getSheetByName('Messages');
const last=m.getLastRow();
const seen={};
if(last>1){
m.getRange(2,1,last-1,1).getValues().forEach(function(r){seen[String(r[0])]=1;});
}
const items=d.items.slice(0,200);
const rows=[];
let dup=0;
let folder=null;
const t=now();
items.forEach(function(it){
const id=String(it.id||'');
if(!id||seen[id]){dup++;return;}
seen[id]=1;
let imageId='';
if(it.image){
try{
if(!folder)folder=DriveApp.getFolderById(props().getProperty('FOLDER_ID'));
const f=folder.createFile(Utilities.newBlob(Utilities.base64Decode(it.image),'image/jpeg',id+'.jpg'));
imageId=f.getId();
}catch(err){imageId='';}
}
const at=Number(it.posted_at)||Date.now();
rows.push([id,String(d.device_id),person,it.app_package||'',it.app_name||'',it.sender||'',it.title||'',String(it.text||'').slice(0,5000),it.conversation||'',String(at),Utilities.formatDate(new Date(at),TZ,'yyyy-MM-dd HH:mm:ss'),imageId,t]);
});
if(rows.length){
const start=m.getLastRow()+1;
const need=start+rows.length-1-m.getMaxRows();
if(need>0)m.insertRowsAfter(m.getMaxRows(),need);
m.getRange(start,1,rows.length,MSG_HEAD.length).setNumberFormat('@').setValues(rows);
}
dev.getRange(row,5,1,2).setValues([[t,t]]);
return {ok:true,saved:rows.length,duplicates:dup};
}

function devices(){
const s=ss().getSheetByName('Devices');
const last=s.getLastRow();
if(last<2)return {ok:true,devices:[]};
const v=s.getRange(2,1,last-1,DEV_HEAD.length).getValues();
return {ok:true,devices:v.map(function(r){
return {device_id:String(r[0]),person:r[1],role:r[2],registered_at:r[3],last_seen:r[4],last_upload:r[5],app_version:r[6]};
})};
}

function list(d){
const m=ss().getSheetByName('Messages');
const last=m.getLastRow();
if(last<2)return {ok:true,items:[],has_more:false,max_seq:1};
const v=m.getRange(2,1,last-1,MSG_HEAD.length).getValues();
const limit=Math.min(Number(d.limit)||200,1000);
const afterSeq=Number(d.after_seq)||0;
const before=Number(d.before)||0;
let items=v.map(function(r,i){
return {seq:i+2,id:String(r[0]),device_id:String(r[1]),person:r[2],app_package:r[3],app_name:r[4],sender:r[5],title:r[6],text:r[7],conversation:r[8],posted_at:Number(r[9]),posted_time:r[10],image_id:r[11],saved_at:r[12]};
});
items=items.filter(function(x){
if(d.device_id&&x.device_id!==String(d.device_id))return false;
if(d.app_package&&x.app_package!==d.app_package)return false;
if(afterSeq&&x.seq<=afterSeq)return false;
if(before&&x.posted_at>=before)return false;
return true;
});
items.sort(function(a,b){return b.posted_at-a.posted_at;});
return {ok:true,items:items.slice(0,limit),has_more:items.length>limit,max_seq:last};
}

function image(d){
if(!d.image_id)return {ok:false,error:'no_image_id'};
try{
const f=DriveApp.getFileById(d.image_id);
const ps=f.getParents();
let inside=false;
while(ps.hasNext()){
if(ps.next().getId()===props().getProperty('FOLDER_ID'))inside=true;
}
if(!inside)return {ok:false,error:'not_found'};
return {ok:true,image:Utilities.base64Encode(f.getBlob().getBytes())};
}catch(err){
return {ok:false,error:'not_found'};
}
}
