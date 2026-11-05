local reservedQty = tonumber(redis.call('HGET', KEYS[3], ARGV[1]) or '0')
if reservedQty > 0 then
    redis.call('INCRBY', KEYS[1], reservedQty)
    redis.call('ZREM', KEYS[2], ARGV[1])
    redis.call('HDEL', KEYS[3], ARGV[1])
    redis.call('HDEL', KEYS[4], ARGV[1])
    redis.call('SREM', KEYS[5], ARGV[2])
    return 1
end

local removedUser = redis.call('SREM', KEYS[5], ARGV[2])
if removedUser == 1 then
    redis.call('INCRBY', KEYS[1], tonumber(ARGV[3]))
    return 1
end
return 0
