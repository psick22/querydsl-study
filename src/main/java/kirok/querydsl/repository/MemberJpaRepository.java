package kirok.querydsl.repository;

import static kirok.querydsl.entity.QMember.member;
import static kirok.querydsl.entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import kirok.querydsl.dto.MemberSearchDto;
import kirok.querydsl.dto.MemberTeamDto;
import kirok.querydsl.dto.QMemberTeamDto;
import kirok.querydsl.entity.Member;
import org.springframework.stereotype.Repository;

@Repository
public class MemberJpaRepository {


    private final EntityManager em;
    private final JPAQueryFactory queryFactory;


    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }
//
//    public Optional<Member> findByIdV2(Long id) {
//
//        return queryFactory.selectFrom(member).where(member.id.eq(id))
//
//    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }

    public List<Member> findAllV2() {
        return queryFactory.selectFrom(member).fetch();
    }

    public List<Member> findByUsername(String username) {
        return em
            .createQuery("select m from Member m where m.username = :username", Member.class)
            .setParameter("username", username).getResultList();

    }

    public List<Member> findByUsernameV2(String username) {

        return queryFactory.selectFrom(member).where(member.username.eq(username)).fetch();

    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchDto condition) {

        BooleanBuilder builder = new BooleanBuilder();

        // StringUtils.hasText => null 또는 "" 검사 
        if (hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }

        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }

        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

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
            .where(builder)
            .fetch();

    }

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

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanBuilder usernameEqV2(String username) {
        return nullSafeBuilder(() -> member.username.eq(username));
    }

    private BooleanBuilder nullSafeBuilder(Supplier<BooleanExpression> f) {
        try {
            return new BooleanBuilder(f.get());
        } catch (Exception e) {
            return new BooleanBuilder();

        }
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


    public List<Member> searchMember(MemberSearchDto condition) {

        return queryFactory
            .select(member)
            .from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEqV2(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageBetween(condition.getAgeLoe(), condition.getAgeGoe())
            )
            .fetch();

    }

    private BooleanExpression ageBetween(Integer ageLoe, Integer ageGoe) {
        return ageLoe(ageLoe).and(ageGoe(ageGoe));
    }

    private BooleanExpression ageBetweenV2(Integer ageLoe, Integer ageGoe) {
        try {
            return ageLoe(ageLoe).and(ageGoe(ageGoe));

        } catch (NullPointerException e) {
            return null;
        }
    }

}
