if redis.call('EXISTS', KEYS[1]) == 0 then
    return -2
end

local expiredOrders = redis.call('ZRANGE', KEYS[2], '-inf', ARGV[4], 'BYSCORE')
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
    redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', ARGV[4])
end

if redis.call('HEXISTS', KEYS[3], ARGV[1]) == 1 then
    return 1
end

if redis.call('SISMEMBER', KEYS[5], ARGV[2]) == 1 then
    return -1
end

local available = tonumber(redis.call('GET', KEYS[1]) or '0')
local reqQty = tonumber(ARGV[3])
if available < reqQty then
    return 0
end

redis.call('DECRBY', KEYS[1], reqQty)
redis.call('ZADD', KEYS[2], ARGV[5], ARGV[1])
redis.call('HSET', KEYS[3], ARGV[1], reqQty)
redis.call('HSET', KEYS[4], ARGV[1], ARGV[2])
redis.call('SADD', KEYS[5], ARGV[2])
if redis.call('TTL', KEYS[5]) < 0 then
    redis.call('EXPIRE', KEYS[5], 172800)
end
return 1
