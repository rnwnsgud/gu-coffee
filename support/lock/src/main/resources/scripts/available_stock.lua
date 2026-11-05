local expiredOrders = redis.call('ZRANGE', KEYS[2], '-inf', ARGV[1], 'BYSCORE')
for _, expiredOrderKey in ipairs(expiredOrders) do
    local expiredQty = tonumber(redis.call('HGET', KEYS[3], expiredOrderKey) or '0')
    local expiredUser = redis.call('HGET', KEYS[4], expiredOrderKey)
    if expiredQty > 0 then
        redis.call('INCRBY', KEYS[1], expiredQty)
    end
    redis.call('HDEL', KEYS[3], expiredOrderKey)
    redis.call('HDEL', KEYS[4], expiredOrderKey)
    if expiredUser then
        redis.call('SREM', KEYS[5], expiredUser)
    end
end
if #expiredOrders > 0 then
    redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', ARGV[1])
end
return tonumber(redis.call('GET', KEYS[1]) or '0')
