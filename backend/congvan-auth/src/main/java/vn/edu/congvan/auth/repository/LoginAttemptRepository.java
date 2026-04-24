package vn.edu.congvan.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.auth.entity.LoginAttemptEntity;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, Long> {}
