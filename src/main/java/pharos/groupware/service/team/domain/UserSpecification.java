package pharos.groupware.service.team.domain;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import pharos.groupware.service.common.enums.UserStatusEnum;
import pharos.groupware.service.team.dto.UserSearchReqDto;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {
    public static Specification<User> search(UserSearchReqDto req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(req.getKeyword())) {
                Predicate usernameLike = cb.like(root.get("username"), "%" + req.getKeyword() + "%");
                Predicate emailLike = cb.like(root.get("email"), "%" + req.getKeyword() + "%");
                predicates.add(cb.or(usernameLike, emailLike));
            }

            if (StringUtils.hasText(req.getRole())) {
                predicates.add(cb.equal(root.get("role"), req.getRole()));
            }

            if (StringUtils.hasText(req.getStatus())) {
                UserStatusEnum statusEnum = UserStatusEnum.valueOf(req.getStatus().toUpperCase());
                predicates.add(cb.equal(root.get("status"), statusEnum));
            }


            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

