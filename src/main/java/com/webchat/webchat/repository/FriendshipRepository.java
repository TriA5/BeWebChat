package com.webchat.webchat.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.webchat.webchat.entity.Friendship;
import com.webchat.webchat.entity.User;
@RepositoryRestResource(path = "friendships")
public interface FriendshipRepository extends JpaRepository<Friendship, UUID>{
    // List<Friendship> findByAddresseeAndStatus(User addressee, String status);
    // List<Friendship> findByRequesterOrAddressee(User requester, User addressee);

    // kiểm tra xem đã có quan hệ giữa 2 user chưa
    Optional<Friendship> findByRequesterAndAddressee(User requester, User addressee);

    // lấy danh sách bạn bè của 1 user
    List<Friendship> findByRequesterOrAddresseeAndStatus(User requester, User addressee, String status);

    List<Friendship> findByRequesterOrAddressee(User requester, User addressee);
    
    // Nếu chỉ muốn lấy PENDING
    List<Friendship> findByAddresseeAndStatus(User addressee, String status);
    //
    // Lấy tất cả friendships đã được chấp nhận, mà user là requester hoặc addressee
    List<Friendship> findByStatusAndRequesterOrStatusAndAddressee(
        String status1, User requester,
        String status2, User addressee
    );
    
    // Hoặc tách ra dễ hiểu hơn
    List<Friendship> findByStatusAndRequester(String status, User requester);
    List<Friendship> findByStatusAndAddressee(String status, User addressee);
}
