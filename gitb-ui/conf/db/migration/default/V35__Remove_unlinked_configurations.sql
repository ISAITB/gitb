delete from configurations where
concat(`system`, '_', `parameter`, '_', `endpoint`) in (select concat(x1.system, '_', x1.parameter, '_', x1.endpoint) from (
select distinct c.system, c.parameter, c.endpoint
from configurations c
join endpoints e on c.endpoint = e.id
left join systemimplementsactors sia on sia.sut_id = c.system and sia.actor_id = e.actor
where sia.actor_id is null) x1);