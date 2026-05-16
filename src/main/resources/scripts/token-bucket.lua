-- Implement thuật toán Token Bucket su dung Lua
-- Tại sao dùng Lua?: Redis chạy Lua script dưới dạng Atomic. Trong lúc script này chạy, không có request nào khác được can thiệp vào Key này. Loại bỏ hoàn toàn lỗi Race Condition (2 request trừ xu cùng một lúc)

local key = KEYS[1] -- Object key: (dihouse:ratelimit:user:mixigaming)
local rate = tonumber(ARGV[1]) -- supply speed (token/s) - Số xu được nhỏ vào xô mỗi giây
local capacity = tonumber(ARGV[2]) -- bucket capacity
local now = tonumber(ARGV[3]) -- Thời gian hiện tại
local requested = tonumber(ARGV[4]) -- 1 request = 1 token

-- HMGET trả về 2 giá trị từ Hash Map: số xu còn lại (tokens) và lần cuối cùng cập nhật (last_refill)
local info = redis.call("HMGET", key, "tokens", "last_refill")
local last_tokens = tonumber(info[1])
local last_refill = tonumber(info[2])

-- Nếu key chưa từng tồn tại (User mới gọi API lần đầu)
-- Khởi tạo xô đầy ắp xu và mốc thời gian là ngay lúc này
if not last_tokens then
	last_tokens = capacity
	last_refill = now
end

-- Tính thời gian đã trôi qua kể từ lần cuối chọc vào xô.
local delta = math.max(0, now - last_refill)

-- Số xu hiện tại = Số xu cũ + (Thời gian trôi qua * Tốc độ)
-- math.min(capacity, ...) đảm bảo Xô không bao giờ trào ra ngoài sức chứa tối đa.
local filled_tokens = math.min(capacity, last_tokens + (delta * rate))

local allowed = false
-- Nếu số xu hiện tại lớn hơn hoặc bằng mức phí yêu cầu -> Đủ tiền qua trạm
if filled_tokens >= requested then
	allowed = true
	filled_tokens = filled_tokens - requested
end

-- Cập nhật lại số xu vừa bị trừ và mốc thời gian "now" để tính cho lần sau
redis.call("HMSET", key, "tokens", filled_tokens, "last_refill", now)

-- Optimize:
-- Phép tính math.ceil(capacity / rate) ra được số giây để cái xô từ 0 xu đầy lên Max.
-- Nhân 2 lần thời gian này lên làm TTL (Time To Live).
-- Nếu quá thời gian này user không gọi API -> Xóa key đi cho nhẹ Redis. Đằng nào gọi lại xô cũng tự đầy.
redis.call("EXPIRE", key, math.ceil(capacity / rate) * 2)

-- true or false
return allowed
