package task_tracker_mail.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import task_tracker_mail.entity.User;

import java.util.List;

@Repository
public interface Users extends JpaRepository<User, Long> {
    List<User> findAll();
}
