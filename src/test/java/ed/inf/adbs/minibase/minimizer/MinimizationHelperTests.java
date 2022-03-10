package ed.inf.adbs.minibase.minimizer;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.dbstructures.Relation;
import ed.inf.adbs.minibase.parser.QueryParser;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MinimizationHelperTests {

    Query query14 = QueryParser.parse("STUDENTNAME(name) :- STUDENT(sid, name, 'INF'), ENROLLMENT(sid, cid), GRADE(cid, uid, semester, mark), ENROLLMENT(sid, 'ADBS'), GRADE('ADBS', x, 'MMXXII', g)");

    @Test
    public void test_getHomomorphismMappingResult_handlesBasicCase() {

        Set<RelationalAtom> baseQuery = new HashSet<>();

        baseQuery.add(new RelationalAtom("STUDENT", Arrays.asList(new StringConstant("ADBS"), new Variable("name"), new StringConstant("INF"))));
        baseQuery.add(new RelationalAtom("ENROLLMENT", Arrays.asList(new Variable("sid"), new Variable("cid"))));
        baseQuery.add(new RelationalAtom("ENROLLMENT", Arrays.asList(new Variable("sid"), new StringConstant("ADBS"))));

        Set<RelationalAtom> correctResultQuery = new HashSet<>();
        correctResultQuery.add(new RelationalAtom("STUDENT", Arrays.asList(new StringConstant("ADBS"), new Variable("name"), new StringConstant("INF"))));
        correctResultQuery.add(new RelationalAtom("ENROLLMENT", Arrays.asList(new Variable("sid"), new StringConstant("ADBS"))));

        Set<RelationalAtom> mappingResult = MinimizationHelpers.getHomomorphismMappingResult(new Variable("cid"), new StringConstant("ADBS"),baseQuery);
        System.out.println(mappingResult);
        assertEquals(mappingResult, correctResultQuery);
    }

    @Test
    public void test_getHomomorphismMappingResult_handlesQuery14CidToADBS() throws IOException {
        Set<RelationalAtom> baseQuery = query14.getBody().stream().map(RelationalAtom.class::cast).collect(Collectors.toSet());
        Set<RelationalAtom> targetQuery = QueryParser.parse("STUDENTNAME(name) :- ENROLLMENT(sid, 'ADBS'), GRADE('ADBS', x, 'MMXXII', g), STUDENT(sid, name, 'INF'), GRADE('ADBS', uid, semester, mark)").getBody().stream().map(RelationalAtom.class::cast).collect(Collectors.toSet());

        System.out.println(baseQuery);

        Set<RelationalAtom> mappingResult = MinimizationHelpers.getHomomorphismMappingResult(new Variable("cid"), new StringConstant("ADBS"),baseQuery);

        System.out.println(targetQuery);
        System.out.println(mappingResult);

        assertEquals(targetQuery, mappingResult);

    }

    @Test
    public void test_backtrackThroughMappings_Query14Subset() throws IOException {

        Set<RelationalAtom> baseQuery = query14.getBody().stream().map(RelationalAtom.class::cast).collect(Collectors.toSet());


        Map<Variable, Set<Term>> transformationsToAttempt = new HashMap<Variable, Set<Term>>();
        transformationsToAttempt.put(new Variable("cid"), new HashSet<Term>() {{
            add(new Variable("sid"));
            add(new StringConstant("ADBS"));
        }});
        transformationsToAttempt.put(new Variable("sid"), new HashSet<Term>(){{
            add(new StringConstant("ADBS"));
        }});

        MappingQueueState state = new MappingQueueState(baseQuery, transformationsToAttempt);
    }

    @Test
    public void test_queryBodiesEquivalent_sameBody() {
        Set<RelationalAtom> baseQuery = query14.getBody().stream().map(RelationalAtom.class::cast).collect(Collectors.toSet());
        Set<RelationalAtom> targetQuery = new HashSet<>(baseQuery);
        assertTrue(MinimizationHelpers.queryBodiesEquivalent(baseQuery, targetQuery));
    }

    @Test
    public void test_queryBodiesEquivalent_diffVarname() {
        Set<RelationalAtom> baseQuery = query14.getBody().stream().map(RelationalAtom.class::cast).collect(Collectors.toSet());
        Set<RelationalAtom> targetQuery = MinimizationHelpers.getHomomorphismMappingResult(new Variable("cid"), new Variable("sid"), baseQuery);

        System.out.println(baseQuery);
        System.out.println(targetQuery);
        assertTrue(MinimizationHelpers.queryBodiesEquivalent(baseQuery,targetQuery));
    }

    public void test_queryBodiesEquivalent_anotherCase() {
        Query test = QueryParser.parse("Q(x) :- R(w, 5, v), R(w, 5, z), R(x, 5, u)");
        Set<RelationalAtom> baseQuery = test.getBody().stream().map(RelationalAtom.class::cast).collect(Collectors.toSet());
        Set<RelationalAtom> targetQuery = MinimizationHelpers.getHomomorphismMappingResult(new Variable("w"), new Variable("sid"), baseQuery);

    }

}
