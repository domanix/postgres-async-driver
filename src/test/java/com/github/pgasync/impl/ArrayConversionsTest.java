package com.github.pgasync.impl;

import com.github.pgasync.Row;
import org.junit.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ArrayConversionsTest {
    @ClassRule
    public static DatabaseRule dbr = new DatabaseRule();


    @BeforeClass
    public static void create() {
        drop();
        dbr.query("CREATE TABLE CA_TEST (" +
            "TEXTA TEXT[], SHORTA INT2[], INTA INT4[], LONGA INT8[], FLOATA FLOAT4[], TIMESTAMPA TIMESTAMP[])");
    }

    @AfterClass
    public static void drop() {
        dbr.query("DROP TABLE IF EXISTS CA_TEST");
    }

    @After
    public void empty() {
        dbr.query("DELETE FROM CA_TEST");
    }

    public Row getRow() {
        return dbr.query("SELECT * FROM CA_TEST").row(0);
    }

    @Test
    public void selectShorts() {
        dbr.query("INSERT INTO CA_TEST (SHORTA) VALUES ('{0, 1, 2, null, 4}')");

        assertArrayEquals(
            new Short[]{0, 1, 2, null, 4},
            getRow().getArray("SHORTA", Short[].class));
    }

    @Test
    public void selectInts() {
        dbr.query("INSERT INTO CA_TEST (INTA) VALUES ('{0, null, 2, 3}')");

        assertArrayEquals(
            new Integer[]{0, null, 2, 3},
            getRow().getArray("INTA", Integer[].class));
    }

    @Test
    public void selectLongs() {
        dbr.query("INSERT INTO CA_TEST (LONGA) VALUES ('{-1, null, 1, 2, 3}')");

        assertArrayEquals(
            new Long[]{-1l, null, 1l, 2l, 3l},
            getRow().getArray("LONGA", Long[].class));
    }

    @Test
    public void selectIntsMulti() {
        dbr.query("INSERT INTO CA_TEST (INTA) VALUES ('{{{0}, {1}}, {{2}, {3}}}')");

        assertArrayEquals(
            new Integer[][][]{
                new Integer[][]{new Integer[]{0}, new Integer[]{1}},
                new Integer[][]{new Integer[]{2}, new Integer[]{3}}},
            getRow().getArray("INTA", Integer[][].class));
    }

    @Test
    public void selectText() {
        dbr.query("INSERT INTO CA_TEST (TEXTA) VALUES ('{foo, bar, \"{foo, bar}\"}')");

        assertArrayEquals(
            new String[]{"foo", "bar", "{foo, bar}"},
            getRow().getArray("TEXTA", String[].class));
    }

    @Test
    public void selectTextMulti() {
        dbr.query("INSERT INTO CA_TEST (TEXTA) VALUES (" +
            "'{{f, o, null}, {b, null, r}, {null, a, z}}')");

        assertArrayEquals(
            new String[][]{
                new String[]{"f", "o", null},
                new String[]{"b", null, "r"},
                new String[]{null, "a", "z"}},
            getRow().getArray("TEXTA", String[][].class));
    }

    @Test
    public void selectFloat() {
        dbr.query("INSERT INTO CA_TEST (FLOATA) VALUES ('{177.7, 0, null, -2.012}')");

        assertArrayEquals(
            new BigDecimal[]{
                new BigDecimal("177.7"),
                new BigDecimal("0"),
                null,
                new BigDecimal("-2.012")
            },
            getRow().getArray("FLOATA", BigDecimal[].class));
    }

    @Test
    public void selectTimestamp() {
        dbr.query("INSERT INTO CA_TEST (TIMESTAMPA) VALUES ('"
        + "{1999-05-16 00:00:00.591, 1970-02-04 01:02:33.01, null}')");

        assertArrayEquals(
            new Timestamp[]{
                new Timestamp(926812800591L),
                new Timestamp(2941353001L),
                null
            },
            getRow().getArray("TIMESTAMPA", Timestamp[].class));
    }

    @Test
    public void roundtripInt() {
        Integer[][] a = new Integer[][]{
            new Integer[]{1, 2, 3},
            new Integer[]{4, 5, 6}
        };
        dbr.query("INSERT INTO CA_TEST (INTA) VALUES ($1)", Arrays.asList(new Object[]{a}));
        assertArrayEquals(
            a,
            dbr.query(
                "SELECT INTA FROM CA_TEST WHERE INTA = $1",
                Arrays.asList(new Object[]{a})).row(0).getArray("INTA", Integer[].class));
    }

    @Test
    public void roundtripUTF8() {
        String[] a = new String[]{"U&\"d\\0061t\\+000061\"", "d\u0061t\u0061\u2301" };
        dbr.query("INSERT INTO CA_TEST (TEXTA) VALUES ($1)", Arrays.asList(new Object[]{a}));
        assertArrayEquals(
            a,
            dbr.query(
                "SELECT TEXTA FROM CA_TEST WHERE TEXTA = $1",
                Arrays.asList(new Object[]{a})).row(0).getArray("TEXTA", String[].class));
    }

    @Test
    public void insertUnboxed() {
        short[][] a = new short[][]{new short[]{0, 1}, new short[]{1, 0}};
        dbr.query("INSERT INTO CA_TEST (INTA) VALUES ($1)", Arrays.asList(new Object[]{a}));
        assertEquals(
            1,
            dbr.query(
                "SELECT INTA FROM CA_TEST WHERE INTA = $1",
                Arrays.asList(new Object[]{a})).size());
    }

    @Test
    public void implicitGet() {
        dbr.query("INSERT INTO CA_TEST (INTA) VALUES ('{1, 2, 3}')");
        PgRow row = (PgRow)dbr.query("SELECT * FROM CA_TEST").row(0);
        assertArrayEquals(new Integer[]{1, 2, 3}, (Object[])row.get("INTA"));
    }
}