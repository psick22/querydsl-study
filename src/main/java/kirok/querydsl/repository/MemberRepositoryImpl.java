package kirok.querydsl.repository;

import static kirok.querydsl.entity.QMember.member;
import static kirok.querydsl.entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import kirok.querydsl.dto.MemberSearchDto;
import kirok.querydsl.dto.MemberTeamDto;
import kirok.querydsl.dto.QMemberTeamDto;

public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchDto condition) {

        return queryFactory
            .select(
                new QMemberTeamDto(
                    member.id,
                    member.username,
                    member.age,
                    team.id,
                    team.name)
            )
            .from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe())
            )
            .fetch();

    }

    private BooleanBuilder nullSafeBuilder(Supplier<BooleanExpression> f) {
        try {
            return new BooleanBuilder(f.get());
        } catch (Exception e) {
            return new BooleanBuilder();

        }
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanBuilder usernameEqV2(String username) {
        return nullSafeBuilder(() -> member.username.eq(username));
    }


    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanBuilder teamNameEqV2(String teamName) {
        return nullSafeBuilder(() -> team.name.eq(teamName));
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanBuilder ageGoeV2(Integer ageGoe) {
        return nullSafeBuilder(() -> member.age.goe(ageGoe));

    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    private BooleanBuilder ageLoeV2(Integer ageLoe) {
        return nullSafeBuilder(() -> member.age.loe(ageLoe));

    }


}
