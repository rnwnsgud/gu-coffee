local expireAt = redis.call('ZSCORE', KEYS[2], ARGV[1])
if not expireAt then
    return 0
end

if tonumber(expireAt) <= tonumber(ARGV[2]) then
    local expiredQty = tonumber(redis.call('HGET', KEYS[3], ARGV[1]) or '0')
    local expiredUser = redis.call('HGET', KEYS[4], ARGV[1])
    if expiredQty > 0 then
        redis.call('INCRBY', KEYS[1], expiredQty)
    end
    redis.call('ZREM', KEYS[2], ARGV[1])
    redis.call('HDEL', KEYS[3], ARGV[1])
    redis.call('HDEL', KEYS[4], ARGV[1])
    if expiredUser then
        redis.call('SREM', KEYS[5], expiredUser)
    end
    return 0
end

redis.call('ZREM', KEYS[2], ARGV[1])
redis.call('HDEL', KEYS[3], ARGV[1])
redis.call('HDEL', KEYS[4], ARGV[1])
return 1
