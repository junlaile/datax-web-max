package com.wugui.datax.admin.tool.database;

import com.wugui.datax.admin.util.AESUtil;
import org.junit.Test;

import java.sql.*;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author junlaile
 * @version 1.0
 * @date 2022/11/14 13:26
 */
public class GainLinkWriteTest {

    @Test
    public void mybatisTest1() {
        String dbDriver = "com.mysql.jdbc.Driver";
        String dbUrl = "jdbc:mysql:///test?serverTimezone=Asia/Shanghai&useLegacyDatetimeCode=false&useSSL=false&nullNamePatternMatchesAll=true&useUnicode=true&characterEncoding=UTF-8";
        String dbUsername = "root";
        String dbPassword = "root";
        Integer max = GainLinkWrite.max(dbDriver, dbUrl, dbUsername, dbPassword,
                "id", "shop_copy", Integer.class);
        System.out.println("max = " + max);
    }
    @Test
    public void mybatisTest2() {
        String dbDriver = "ru.yandex.clickhouse.ClickHouseDriver";
        String dbUrl = "jdbc:clickhouse://116.63.149.40:8123/geo";
        String dbUsername = "geography";
        String dbPassword = "geography";
        Timestamp max = GainLinkWrite.max(dbDriver, dbUrl, dbUsername, dbPassword,
                "create_time", "fail_stop", Timestamp.class);
        System.out.println("max = " + max.getTime());
    }

    @Test
    public void AESUtilDemo() {
        String decrypt = AESUtil.decrypt("yRjwDFuoPKlqya9h9H2Amg==");
        System.out.println(decrypt);
    }

    @Test
    public void cutDemo() {
        String str = "-DupdateTime='%s' -F=update_time -DcreateTime='%s'";
        String space = " ";
        String[] split = str.split(space);
        String fileName = "";
        StringBuilder strC = new StringBuilder();
        for (String s : split) {
            if (s.startsWith("-F")) {
                fileName = s;
            } else {
                strC.append(s).append(space);
            }
        }
        String strD = strC.substring(0, strC.length() - 1);
        System.out.println("strC = " + strD);
        System.out.println("fileName = " + fileName);
    }

    @Test
    public void enumDemo(){
        DataBaseType[] values = DataBaseType.values();
        for (DataBaseType value : values) {
            System.out.println("value = " + value.getTypeName());
        }
    }

}