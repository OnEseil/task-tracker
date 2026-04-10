package task_tracker.auth.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import task_tracker.auth.entity.Token;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    @EntityGraph(attributePaths = {"user"})
    Token findTokenByUserId(Long user_id);
}
