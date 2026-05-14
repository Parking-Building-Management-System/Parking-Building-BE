SMART PARKING CORE: BaseEntity, UUIDv7 & Bức Màn Bí Mật Của Hibernate

NOTE: Dừng lại 5 phút đọc kỹ document này

1. Cơ chế hoạt động của JPA & Hibernate (Dành cho người mới)

Đa số newbie hay nghĩ: "Code Java gọi hàm save() -> Spring Boot gọi SQL INSERT xuống DB". SAI HOÀN TOÀN.

Trong Spring Data JPA, chúng ta đang làm việc với Hibernate (thằng implement cái chuẩn JPA). Hibernate có một khái niệm cốt lõi gọi là Persistence Context (hay còn gọi là L1 Cache).

Hãy tưởng tượng Persistence Context là một cái "Bàn làm việc":

    Tìm 1 object bằng findById(): Hibernate lên kho (Database) lấy cuốn sổ xuống, đặt lên bàn. Cuốn sổ lúc này đang ở trạng thái Managed (Persistent).

    Gọi hàm setName("Độ Mixi") vào cuốn sổ đang trên bàn. KHÔNG CẦN gọi repository.save(). Khi Transaction kết thúc (hàm chạy xong), Hibernate sẽ tự nhìn lướt qua cái bàn (cơ chế Dirty Checking), thấy cuốn sổ bị đổi tên, nó sẽ tự động sinh ra câu lệnh UPDATE vứt xuống DB.

    Lazy Loading (Hibernate Proxy):Khi lấy ra 1 thằng Tenant, Hibernate mặc định sẽ không móc bảng User (danh sách nhân viên) lên theo để tiết kiệm RAM. Nó nhét vào đó một cái vỏ bọc giả (gọi là Proxy). Chỉ khi nào code gọi tenant.getUsers(), cái Proxy đó mới lén chạy câu lệnh SELECT thứ 2 xuống DB để lấy data.

2. Bí ẩn của BaseEntity: Tại sao không dùng Lombok @EqualsAndHashCode?

Thay vì bảng nào cũng phải gõ lại id, createdAt, updatedAt, chúng ta gom nó lại vào BaseEntity và dùng annotation @MappedSuperclass. Các class khác chỉ việc extends nó là xong.

Nhưng phần "đắt giá" nhất nằm ở hai hàm equals() và hashCode(). Tuyệt đối KHÔNG ĐƯỢC dùng @EqualsAndHashCode của Lombok cho Entity. Tại sao?
Vấn đề 1: Lỗi "Vào HashSet rồi mất tích"

Nếu dùng Lombok, hàm hashCode() sẽ được tính dựa trên tất cả các field, bao gồm cả id.

    Ta tạo một User mới tinh (id = null). Thêm nó vào một cái HashSet. HashCode lúc này là A.

    Lưu User xuống DB. Nó được cấp id = 123.

    Lúc này, HashCode của nó bị đổi thành B.

    Khi vào HashSet tìm lại thằng User đó? Bộ nhớ sẽ trả về null vì HashCode đã thay đổi! Thằng User kẹt lại trong bộ nhớ gây Memory Leak.

Giải pháp: Trong BaseEntity, hashCode() luôn trả về giá trị cố định (constant) của class type.

Vấn đề 2: Lừa đảo thị giác với Hibernate Proxy

Nếu hai object so sánh với nhau bằng getClass() == o.getClass(), nó sẽ tạch khi đụng phải Lazy Loading. Bởi vì class của đối tượng lấy từ DB lười lên không phải là User, mà là User$HibernateProxy$xyz.

Giải pháp: Trong hàm equals() của chúng ta đã viết sẵn logic bóc tách vỏ Proxy instanceof HibernateProxy để móc class lõi ra so sánh.

3. UUIDv7: Kẻ Cứu Rỗi B-Tree Index

Chúng ta dùng UUID thay vì Long (Auto Increment) để bảo mật hệ thống (tránh việc hacker đoán được số lượng bãi xe hay user bằng cách tăng ID lên 1). Nhưng tại sao lại là UUIDv7 mà không phải UUIDv4 mặc định của Java?

Hãy hiểu cách Database lưu trữ (B-Tree):
PostgreSQL lưu các index theo cấu trúc cây B-Tree. Nó thích những dữ liệu được chèn vào một cách tuần tự (tăng dần).

    Thảm họa UUIDv4: Nó được sinh ra hoàn toàn ngẫu nhiên. Lúc thì bắt đầu bằng A, lúc thì F, lúc thì 1. Khi nhét vào DB, Postgres phải liên tục chẻ các node của B-Tree ra làm đôi (Page Splitting) để nhét ID mới vào giữa. Hậu quả: Dữ liệu phân mảnh, DB chạy cực kỳ chậm khi hệ thống phình to.

    Quyền năng UUIDv7: Nó cấu tạo gồm 2 phần: [Timestamp 48-bit] + [Random]. Vì phần đầu luôn là thời gian hiện tại, nên UUIDv7 được sinh ra luôn tăng dần theo thời gian. Postgres chỉ việc nối nó vào cuối cây B-Tree một cách êm ái, tốc độ insert nhanh không kém gì số tự tăng!

Cách sử dụng

Mọi Entity trong hệ thống BẮT BUỘC phải kế thừa BaseEntity. Không được tự ý định nghĩa @Id riêng.

@Entity
@Table(name = "tenants")
// Chỉ xài @Getter, @Setter, KHÔNG xài @Data, KHÔNG xài @EqualsAndHashCode
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String name;

}
