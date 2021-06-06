package kirok.querydsl.repository;

import static kirok.querydsl.entity.QMember.member;
import static kirok.querydsl.entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import kirok.querydsl.dto.MemberSearchDto;
import kirok.querydsl.dto.MemberTeamDto;
import kirok.querydsl.dto.QMemberTeamDto;
import kirok.querydsl.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;

public class MemberRepositoryImpl extends QuerydslRepositorySupport implements
    MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;
//
//    public MemberRepositoryImpl(EntityManager em) {
//        this.queryFactory = new JPAQueryFactory(em);
//    }


    public MemberRepositoryImpl(EntityManager em) {
        super(Member.class);
        this.queryFactory = new JPAQueryFactory(em);
    }


    @Override
    public List<MemberTeamDto> search(MemberSearchDto condition) {

        return from(member)
            .leftJoin(member)
            .where(
                usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe())
            )
            .select(
                new QMemberTeamDto(
                    member.id,
                    member.username,
                    member.age,
                    team.id,
                    team.name)
            )
            .fetch();

//        return queryFactory
//            .select(
//                new QMemberTeamDto(
//                    member.id,
//                    member.username,
//                    member.age,
//                    team.id,
//                    team.name)
//            )
//            .from(member)
//            .leftJoin(member.team, team)
//            .where(
//                usernameEq(condition.getUsername()),
//                teamNameEq(condition.getTeamName()),
//                ageGoe(condition.getAgeGoe()),
//                ageLoe(condition.getAgeLoe())
//            )
//            .fetch();

    }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchDto condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
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
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetchResults();

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    public Page<MemberTeamDto> searchPageSimple2(MemberSearchDto condition, Pageable pageable) {
        JPQLQuery<MemberTeamDto> jpaQuery = from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe())
            )
            .select(
                new QMemberTeamDto(
                    member.id,
                    member.username,
                    member.age,
                    team.id,
                    team.name)
            );

        JPQLQuery<MemberTeamDto> query = getQuerydsl()
            .applyPagination(pageable, jpaQuery);

        QueryResults<MemberTeamDto> results = query.fetchResults();
        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchDto condition, Pageable pageable) {

        List<MemberTeamDto> content = queryFactory
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
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPAQuery<Member> countQuery = queryFactory
            .selectFrom(member)
            .leftJoin(member.team, team)
            .where(
                usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe())
            );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);

//        return new PageImpl<>(content, pageable, total);

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
