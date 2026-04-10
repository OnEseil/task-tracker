package task_tracker_mail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import task_tracker_mail.entity.Status;
import task_tracker_mail.entity.Task;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyReportsRepository extends JpaRepository<Task, Long> {

    long countByStatusAndUserId(Status status, Long user_id);

    long countByStatusAndUserIdAndCompletedAt(Status status, Long user_id, LocalDate completedAt);


    List<Task> findTop5ByStatusAndUserId(Status status, Long userId);

    List<Task> findTop5ByStatusAndUserIdAndCompletedAt(Status status, Long userId, LocalDate completedAt);
}
