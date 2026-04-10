package task_tracker.tasks.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import task_tracker.tasks.entity.Task;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query("SELECT t FROM Task t JOIN FETCH t.user WHERE t.user.email = :email")
    List<Task> findByUserEmail(String email);

    Optional<Task> findTaskById(Long id);
}
