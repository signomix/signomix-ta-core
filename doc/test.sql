SELECT d.eui, d.name, d.userid, d.type, d.team, d.channels, d.code, d.decoder,d.devicekey, d.description, 
d.tinterval, d.template, d.pattern, d.commandscript, d.appid,d.groups, d.devid, d.appeui, d.active, d.project, 
d.latitude, d.longitude, d.altitude, d.retention, d.administrators,d.framecheck, d.configuration, d.organization, 
d.organizationapp, a.configuration, false as writable 
FROM devices AS d  
LEFT JOIN applications AS a 
ON d.organizationapp=a.id 
AND EXISTS 
(SELECT DISTINCT ON (ds.eui) ds.eui, ds.ts FROM devicestatus AS ds 
WHERE ds.eui=d.eui AND ds.alert<2 AND ds.tinterval>0 AND ds.ts < TIMESTAMPADD('MILLISECOND', -1*ds.tinterval, CURRENT_TIMESTAMP) }
ORDER BY ds.eui, ds.ts DESC)


SELECT d.eui, d.name, d.userid, d.type, d.team, d.channels, d.code, d.decoder,d.devicekey, d.description, 
d.tinterval, d.template, d.pattern, d.commandscript, d.appid,d.groups, d.devid, d.appeui, d.active, d.project, 
d.latitude, d.longitude, d.altitude, d.retention, d.administrators,d.framecheck, d.configuration, d.organization, 
d.organizationapp, a.configuration, (d.userid=? OR d.administrators like ?) AS writable 
FROM devices AS d  
LEFT JOIN applications AS a 
ON d.organizationapp=a.id 
WHERE (d.userid=? OR d.team like ? OR d.administrators like ?)