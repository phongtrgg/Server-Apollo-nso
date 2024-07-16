package threading;

import cache.ArrowPaint;
import cache.EffectCharPaint;
import cache.EffectInfoPaint;
import cache.ItemCache;
import cache.MapCache;
import cache.MobCache;
import cache.NpcCache;
import cache.Part;
import cache.PartImage;
import cache.SkillInfoPaint;
import cache.SkillOptionTemplates;
import cache.SkillPaint;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import lombok.val;
import patch.*;
import patch.battle.ClanBattleData;
import patch.battle.GBattle;
import patch.clan.ClanThanThu;
import real.*;

import java.io.*;

import server.*;
import real.User;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.json.simple.JSONValue;
import org.json.simple.JSONArray;

import static real.ItemData.*;
import static real.ItemData.getListItemByMaxLevel;
import static server.GameScr.LAT_HINH_ID;
import static server.util.concatArray;

@SuppressWarnings("ALL")
public class Manager {

    public static int TIME_MAINTAIN = 5;
    public static int BOSS_WAIT_TIME_UNIT;
    public static int MIN_YEN_BOSS;
    public static int MAX_YEN_BOSS;
    public static int TIME_DISCONNECT = 10;

    public int PORT;
    public static String host;
    public static String mysql_host;
    public static String mysql_database;
    public static String mysql_user;
    public static String mysql_pass;
    public static byte vsData;
    public static byte vsMap;
    public static byte vsSkill;
    public static byte vsItem;
    public byte[][] tasks;
    private byte[][] maptasks;
    static Server server;
    public RotationLuck[] rotationluck;
    public byte EVENT;
    public String[] NinjaS;
    public static short MAX_CLIENT;

    public static byte MAX_PERCENT = 100;
    public static byte N_YEN;
    public static byte PERCENT_TA_TL = 50;
    public static long YEN_TL;
    public static long YEN_TA;
    public static short[] LANG_CO_ITEM_IDS;
    public static short[] MAP_MOI_IDS;
    public static short[] MAP_MOI_ITEM_IDS;
    public static short[] MAP_SHINBA;
    public static short[] VDMQ_ITEM_IDS;
    public static short[] EMPTY = new short[0];

    public static long MIN_TIME_REFRESH_MOB;
    public static long MIN_TIME_REFRESH_BOSS;
    public static short[] BOSS_ITEM_LV45;
    public static short[] BOSS_ITEM_LV55;
    public static short[] BOSS_ITEM_LV60;
    public static short[] BOSS_ITEM_LV65;
    public static short[] BOSS_ITEM_LV75;
    public static short[] BOSS_ITEM_LV99;
    public static short[] BOSS_ITEM_LV90;
    public static short[] BOSS_ITEM_LV100;
    public static short[] BOSS_ITEM_LV110;
    public static short[] BOSS_LC_ITEM;
    public static short[] BOSS_MAPMOI_ITEM;
    public static short[] BOSS_SUKIEN_ITEM;
    public static short[] BOSS_DEFAULT_ITEM;
    public static short[] BOSS_VUI_XUAN;
    public static short[] BOSS_TUAN_THU;
    public static byte PERCENT_DAME_BOSS;

    public static long TIME_DESTROY_MAP;
    public static short N_ITEM_BOSS;
    public static int Server_WAIT_TIME_UNIT;
    public static short[] ID_FEATURES;
    public static short[] IDS_THUONG_LV70;
    public static short[] IDS_THUONG_LV90;
    public static short[] IDS_THUONG_LV130;
    public static short MULTI_EXP;
    public static short N_THREAD_STOP;
    public static short MAX_CLIENT_PER_SOCKET;

    public static short[] MOMENT_REFRESH_BATTLE;

    public static short[] LDGT_REWARD_ITEM_ID;

    //Cache
    public static MapCache[] mapCache;
    public static NpcCache[] npcCache;
    public static ItemOptionTemplate[] iOptionTemplates;
    public static ItemCache[] itemTemplates;
    public static MobCache[] mobCache;
    public static ArrayList<ArrowPaint> arrs;
    public static ArrayList<int[]> smallImg;
    public static ArrayList<EffectCharPaint> efs;
    public static ArrayList<SkillPaint> sks;
    public static ArrayList<Part> parts;
    public static SkillOptionTemplates[] sOptionTemplates;
    public static NpcTemplate[] npcs;
    /**
     * 0 MIN 1 MAX
     */
    public static int[] MIN_MAX_YEN_RUONG_MAY_MAN = new int[2];
    public static int[] MIN_MAX_YEN_RUONG_TINH_SAO = new int[2];
    public static int[] MIN_MAX_YEN_RUONG_MA_QUAI = new int[2];

    public Manager() {
        entrys = new HashMap<>();
        arrs = new ArrayList<>();
        efs = new ArrayList<>();
        smallImg = new ArrayList<>();
        parts = new ArrayList();
        sks = new ArrayList<>();
        ItemSell.entrys = new ConcurrentHashMap<>();
        this.rotationluck = new RotationLuck[2];
        this.NinjaS = new String[]{"Chưa vào lớp", "Ninja Kiếm", "Ninja Phi Tiêu", "Ninja Kunai", "Ninja Cung", "Ninja Đao", "Ninja Quạt"};
        preload();
    }

    public void preload() {
        this.loadConfigFile();
        this.EVENT = 0;
        if (this.rotationluck[0] == null) {
            this.rotationluck[0] = new RotationLuck("Vòng xoay vip", (byte) 0, (short) 120, 1000000, 50000000, 1000000000);
            this.rotationluck[0].start();
        }
        if (rotationluck[1] == null) {
            this.rotationluck[1] = new RotationLuck("Vòng xoay lượng", (byte) 1, (short) 120, 1000, 10000, 500000, "lượng");
            this.rotationluck[1].start();
        }
        this.loadSkillFromData();
        this.loadCache();
        this.loadDataBase();
        this.loadProperties();
        //buffalo.loadBufffalo();
        this.loadVersion();
        this.loadFileToJar();
    }

    public static long TIME_REFRESH_MOB;
    public static long TIME_REFRESH_BOSS;
    public static byte MIN_DA_LV;
    public static byte N_DA;

    public static String[] MENU_EVENT_NPC;
    public static short ID_EVENT_NPC;
    public static String[] EVENT_NPC_CHAT;

    private void loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream("application.properties")) {

            // load properties from file
            properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            MAX_PERCENT = Byte.parseByte(properties.getProperty("drop-item-percent"));
            PERCENT_TA_TL = Byte.parseByte(properties.getProperty("percent-ta-tl"));
            YEN_TA = Long.parseLong(properties.getProperty("yen-ta"));
            YEN_TL = Long.parseLong(properties.getProperty("yen-tl"));
            TIME_REFRESH_MOB = 1000 * Long.parseLong(properties.getProperty("time-refresh-mob"));
            MIN_DA_LV = Byte.parseByte(properties.getProperty("min-da-lv"));
            N_YEN = Byte.parseByte(properties.getProperty("n-yen"));
            LANG_CO_ITEM_IDS = parseShortArray(properties.getProperty("lang-co-item-ids"));
            VDMQ_ITEM_IDS = parseShortArray(properties.getProperty("vdmq-item-ids"));
            N_DA = Byte.parseByte(properties.getProperty("n-da"));
            MAP_SHINBA= getItemShinba((short)10000);
            MIN_YEN_BOSS = Integer.parseInt(properties.getProperty("min-yen-boss"));
            MAX_YEN_BOSS = Integer.parseInt(properties.getProperty("max-yen-boss"));
            BOSS_WAIT_TIME_UNIT = Integer.parseInt(properties.getProperty("boss-wait-time-unit"));
            Server_WAIT_TIME_UNIT = Integer.parseInt(properties.getProperty("server-wait-time-unit"));
            BOSS_ITEM_LV45 = concatArray(parseShortArray(properties.getProperty("boss-item-lv45")),
                    ItemData.getItemIdByLevel(50, (byte) 1, (byte) 2));
            BOSS_ITEM_LV55 = concatArray(parseShortArray(properties.getProperty("boss-item-lv55")), ItemData.getItemIdByLevel(60, (byte) 1, (byte) 2));
            BOSS_ITEM_LV60 = concatArray(parseShortArray(properties.getProperty("boss-item-lv60")), ItemData.getItemIdByLevel(70, (byte) 1, (byte) 2));
            BOSS_ITEM_LV65 = concatArray(parseShortArray(properties.getProperty("boss-item-lv65")), ItemData.getItemIdByLevel(70, (byte) 1, (byte) 2));
            BOSS_ITEM_LV75 = concatArray(parseShortArray(properties.getProperty("boss-item-lv75")), ItemData.getItemIdByLevel(80, (byte) 1, (byte) 2));
            BOSS_ITEM_LV99 = concatArray(parseShortArray(properties.getProperty("boss-item-lv99")), ItemData.getItemIdByLevel(80, (byte) 1, (byte) 2));
            BOSS_ITEM_LV90 = concatArray(parseShortArray(properties.getProperty("boss-item-lv90")), ItemData.getItemIdByLevel(100, (byte) 1, (byte) 2));
            BOSS_ITEM_LV100 = concatArray(parseShortArray(properties.getProperty("boss-item-lv100")), ItemData.getItemIdByLevel(100, (byte) 1, (byte) 2));
            BOSS_ITEM_LV110 = concatArray(parseShortArray(properties.getProperty("boss-item-lv110")), ItemData.getItemIdByLevel(100, (byte) 1, (byte) 2));
            BOSS_DEFAULT_ITEM = parseShortArray(properties.getProperty("boss-default-item"));

            BOSS_LC_ITEM = parseShortArray(properties.getProperty("boss-item-lc"));
            N_ITEM_BOSS = Short.parseShort(properties.getProperty("n-items-boss"));
            IDS_THUONG_LV70 = parseShortArray(properties.getProperty("thuong-lv70"));
            IDS_THUONG_LV90 = parseShortArray(properties.getProperty("thuong-lv90"));
            IDS_THUONG_LV130 = parseShortArray(properties.getProperty("thuong-lv130"));

            Server.MOMENT_BOSS_REFRESH = parseShortArray(properties.getProperty("moment-boss-refresh"));
            Server.Server_REFRESH = parseShortArray(properties.getProperty("server_refresh"));
            MULTI_EXP = Short.parseShort(properties.getProperty("multi-exp"));
            TIME_MAINTAIN = Integer.parseInt(properties.getProperty("time-bao-tri"));
            N_THREAD_STOP = Short.parseShort(properties.getProperty("n-thread-stop"));

            TIME_DESTROY_MAP = Integer.parseInt(properties.getProperty("time-Destroy-Map"));
            MAX_CLIENT = Short.parseShort(properties.getProperty("max-Client"));
            TIME_DISCONNECT = Short.parseShort(properties.getProperty("time-Disconnect"));

            Resource.TIME_REMOVE_RESOURCE = Long.parseLong(properties.getProperty("time-remove-resource")) * 60000;
            User.DIFFER_USE_ITEM_TIME = Short.parseShort(properties.getProperty("differ-use-item-time"));
            User.MAX_USE_ITEM_FAST = Short.parseShort(properties.getProperty("max-use-item-fast"));
            PERCENT_DAME_BOSS = Byte.parseByte(properties.getProperty("percent-dame-boss"));
            MOMENT_REFRESH_BATTLE = parseShortArray(properties.getProperty("moment-refresh-battle"));
            GBattle.WATING_TIME = Integer.parseInt(properties.getProperty("waiting-time"));
            GBattle.START_TIME = Long.parseLong(properties.getProperty("start-time"));

            EVENT = Byte.parseByte(properties.getProperty("event-type"));
            Body.PERCENT_DAME_PEOPLE = Short.parseShort(properties.getProperty("damage-people-rate"));
            EventItem.entrys = Mapper.converter.readValue(properties.getProperty("event-input" + EVENT), EventItem[].class);

            User.MIN_TIME_RESET_POINT = Long.parseLong(properties.getProperty("min-time-reset-point"));
            Server.TIME_SLEEP_SHINWA_THREAD = Long.parseLong(properties.getProperty("time-run-shinwa-thread"));
            LAT_HINH_ID = parseShortArray(properties.getProperty("lat-hinh-id"));

            MIN_MAX_YEN_RUONG_MAY_MAN = parseIntArray(properties.getProperty("yen-ruong-may-man"));
            MIN_MAX_YEN_RUONG_TINH_SAO = parseIntArray(properties.getProperty("yen-ruong-tinh-sao"));
            MIN_MAX_YEN_RUONG_MA_QUAI = parseIntArray(properties.getProperty("yen-ruong-ma-quai"));
            LDGT_REWARD_ITEM_ID = parseShortArray(properties.getProperty("ldgt-reward-item-id"));
            GameScr.ArryenLuck = parseIntArray(properties.getProperty("yen-lat-hinh"));
            util.setDebug(Boolean.parseBoolean(properties.getProperty("debug")));

            ITEM_LV_10 = getListItemByMaxLevel(10, MAX_PERCENT, N_YEN, N_DA);
            ITEM_LV_20 = getListItemByMaxLevel(20, MAX_PERCENT, N_YEN, N_DA);
            ITEM_LV_30 = getListItemByMaxLevel(30, MAX_PERCENT, N_YEN, N_DA);
            ITEM_LV_40 = getListItemByMaxLevel(40, MAX_PERCENT, N_YEN, N_DA);
            ITEM_LV_50 = getListItemByMaxLevel(50, MAX_PERCENT, N_YEN, N_DA);
            ITEM_LV_60 = getListItemByMaxLevel(60, MAX_PERCENT, N_YEN, N_DA);
            ITEM_LV_70 = getListItemByMaxLevel(70, MAX_PERCENT, N_YEN, N_DA);
            ITEM_LV_80 = getListItemByMaxLevel(80, MAX_PERCENT, N_YEN, N_DA);
            ITEM_LV_90 = getListItemByMaxLevel(90, MAX_PERCENT, N_YEN, N_DA);
            ITEM_LV_100 = getListItemByMaxLevel(100, MAX_PERCENT, N_YEN, N_DA);
            GameScr.LAT_HINH_LV100_ID = util.concatArray(ItemData.ITEM_LV_100, LAT_HINH_ID);
            GameScr.LAT_HINH_LV90_ID = util.concatArray(ItemData.ITEM_LV_90, LAT_HINH_ID);
            GameScr.LAT_HINH_LV80_ID = util.concatArray(ItemData.ITEM_LV_80, LAT_HINH_ID);
            GameScr.LAT_HINH_LV70_ID = util.concatArray(ItemData.ITEM_LV_70, LAT_HINH_ID);
            GameScr.LAT_HINH_LV60_ID = util.concatArray(ItemData.ITEM_LV_60, LAT_HINH_ID);
            GameScr.LAT_HINH_LV50_ID = util.concatArray(ItemData.ITEM_LV_50, LAT_HINH_ID);
            GameScr.LAT_HINH_LV40_ID = util.concatArray(ItemData.ITEM_LV_40, LAT_HINH_ID);
            GameScr.LAT_HINH_LV30_ID = util.concatArray(ItemData.ITEM_LV_30, LAT_HINH_ID);
            GameScr.LAT_HINH_LV20_ID = util.concatArray(ItemData.ITEM_LV_20, LAT_HINH_ID);
            GameScr.LAT_HINH_LV10_ID = util.concatArray(ItemData.ITEM_LV_10, LAT_HINH_ID);

        } catch (IOException e) {
            e.printStackTrace();
            Server.getInstance().stop();
        }
    }
private short[] getItemShinba(short num) {
        short[] result = new short[num];
        Random random = new Random();
        int[] arrCTT = new int[2];
        int[] arrBima = new int[50];
        int[] arrMS = new int[50];
        
        int[] arrYen = new int[2000];
        int[] arrTTTS = new int[15];
        int[] arrTTTT = new int[5];
        int[] arrTTTC = new int[1];
        int[] co = new int[200];
        int[] arrtbx = new int[8];
        int[] danangluong = new int[30];
        int[] arrExpXeSoi = new int[40];
        int[] arrExpXeSoi2X = new int[150];
        int[] arrDaMaThuat = new int[50];
        int[] arrPtl = new int[3];
        int[] arrDDV = new int[10];
        int[] arrCT = new int[60];
        int[] listCT = {775, 788, 789, 0};
        int[] tbXeSoi = {439, 440, 441, 442, 486, 487, 488, 489};
        int[] expXeSoi = {573, 574, 575, 576, 577, 578};
        int[] listdanangluong = {656, 657, 658};
        //da 5 6 7 
        int[] expSoi2x = {4, 5, 6, 0};
        for (int i = 0; i < num; i++) {
            result[i] = (short) -1;
            //rd yen
            for (int j = 0; j < arrYen.length; j++) {
                int yen = random.nextInt(num);
                arrYen[j] = yen;
            }
            for (int a = 0; a < arrYen.length; a++) {
                if (i == arrYen[a]) {
                    result[i] = (short) Integer.parseInt("12");
                }
            }
            for (int j = 0; j < arrDDV.length; j++) {
                int ddv = random.nextInt(num);
                arrDDV[j] = ddv;
            }
            for (int a = 0; a < arrDDV.length; a++) {
                if (i == arrDDV[a]) {
                    result[i] = (short) Integer.parseInt("695");
                }
            }
            //rd exp xe soi
            for (int j = 0; j < arrExpXeSoi.length; j++) {
                int expXS = random.nextInt(num);
                arrExpXeSoi[j] = expXS;
            }
            for (int a = 0; a < arrExpXeSoi.length; a++) {
                int b = random.nextInt(expXeSoi.length);
                if (i == arrExpXeSoi[a]) {
                    result[i] = (short) expXeSoi[b];
                }
            }
            //rd cai trang
            for (int j = 0; j < arrCT.length; j++) {
                int ct = random.nextInt(num);
                arrCT[j] = ct;
            }
            for (int a = 0; a < arrCT.length; a++) {
                int b = random.nextInt(listCT.length);
                if (i == arrCT[a]) {
                    result[i] = (short) listCT[b];
                }
            }
            
            for (int j = 0; j < danangluong.length; j++) {
                int ct = random.nextInt(num);
                danangluong[j] = ct;
            }
            for (int a = 0; a < danangluong.length; a++) {
                int b = random.nextInt(listdanangluong.length);
                if (i == danangluong[a]) {
                    result[i] = (short) listdanangluong[b];
                }
            }
            
            //rd ttts
            for (int j = 0; j < arrTTTS.length; j++) {
                int ttts = random.nextInt(num);
                arrTTTS[j] = ttts;
            }
            for (int a = 0; a < arrTTTS.length; a++) {
                if (i == arrTTTS[a]) {
                    result[i] = (short) Integer.parseInt("455");
                }
            }
            //rd tttt
            for (int j = 0; j < arrTTTT.length; j++) {
                int tttt = random.nextInt(num);
                arrTTTT[j] = tttt;
            }
            for (int a = 0; a < arrTTTT.length; a++) {
                if (i == arrTTTT[a]) {
                    result[i] = (short) Integer.parseInt("456");
                }
            }

            for (int j = 0; j < arrTTTC.length; j++) {
                int tttc = random.nextInt(num);
                arrTTTC[j] = tttc;
            }
            for (int a = 0; a < arrTTTC.length; a++) {
                if (i == arrTTTC[a]) {
                    result[i] = (short) Integer.parseInt("455");
                }
            }

            //rd dá 7
            for (int j = 0; j < arrDaMaThuat.length; j++) {
                int dmt = random.nextInt(num);
                arrDaMaThuat[j] = dmt;
            }
            for (int a = 0; a < arrDaMaThuat.length; a++) {
                if (i == arrDaMaThuat[a]) {
                    result[i] = (short) Integer.parseInt("6");
                }
            }
            //rd ptl
            for (int j = 0; j < arrPtl.length; j++) {
                int ptl = random.nextInt(num);
                arrPtl[j] = ptl;
            }
            for (int a = 0; a < arrPtl.length; a++) {
                if (i == arrPtl[a]) {
                    result[i] = (short) Integer.parseInt("545");
                }
            }
            //rd ctt
            for (int j = 0; j < arrCTT.length; j++) {
                int ctt = random.nextInt(num);
                arrCTT[j] = ctt;
            }
            for (int a = 0; a < arrCTT.length; a++) {
                if (i == arrCTT[a]) {
                    result[i] = (short) Integer.parseInt("454");
                }
            }
            //bí ma
             for (int j = 0; j < arrBima.length; j++) {
                int bima = random.nextInt(num);
                arrBima[j] = bima;
            }
            for (int a = 0; a < arrBima.length; a++) {
                if (i == arrBima[a]) {
                    result[i] = (short) Integer.parseInt("1037");
                }
            }
            //ms
            for (int j = 0; j < arrMS.length; j++) {
                int ms = random.nextInt(num);
                arrMS[j] = ms;
            }
            for (int a = 0; a < arrMS.length; a++) {
                if (i == arrMS[a]) {
                    result[i] = (short) Integer.parseInt("1045");
                }
            }
            //rd co
            for (int j = 0; j < co.length; j++) {
                int tttc = random.nextInt(num);
                co[j] = tttc;
            }
            for (int a = 0; a < co.length; a++) {
                if (i == co[a]) {
                    result[i] = (short) Integer.parseInt("778");
                }
            }
            //rd trang bị xe soi
            for (int j = 0; j < arrtbx.length; j++) {
                int tb = random.nextInt(num);
                arrtbx[j] = tb;
            }
            for (int a = 0; a < arrtbx.length; a++) {
                int b = random.nextInt(tbXeSoi.length);
                if (i == arrtbx[a]) {
                    result[i] = (short) tbXeSoi[b];
                }
            }

            //rd exp soi 2x
            for (int j = 0; j < arrExpXeSoi2X.length; j++) {
                int expXS = random.nextInt(num);
                arrExpXeSoi2X[j] = expXS;
            }
            for (int a = 0; a < arrExpXeSoi2X.length; a++) {
                int b = random.nextInt(expSoi2x.length);
                if (i == arrExpXeSoi2X[a]) {
                    result[i] = (short) expSoi2x[b];
                }
            }
        }
        return result;
    }
    private short[] getItemMapMoi(short num) {
        short[] result = new short[num];
        Random random = new Random();
        int[] arrCTT = new int[2];
        int[] arrYen = new int[2000];
        int[] arrTTTS = new int[15];
        int[] arrTTTT = new int[4];

        int[] co = new int[150];
        int[] arrtbx = new int[12];
        int[] arrExpXeSoi = new int[100];
        int[] arrExpXeSoi2X = new int[200];
        int[] arrDaMaThuat = new int[75];
        int[] arrPtl = new int[2];
        int[] tbXeSoi = {439, 440, 441, 442, 486, 487, 488, 489, 0};
        int[] expXeSoi = {573, 574, 575, 576, 577, 578, 0};

        int[] expSoi2x = {449, 450, 451, 452, 453, 0};
        for (int i = 0; i < num; i++) {
            result[i] = (short) -1;
            //rd yen
            for (int j = 0; j < arrYen.length; j++) {
                int yen = random.nextInt(num);
                arrYen[j] = yen;
            }
            for (int a = 0; a < arrYen.length; a++) {
                if (i == arrYen[a]) {
                    result[i] = (short) Integer.parseInt("12");
                }
            }
            //rd exp xe soi
            for (int j = 0; j < arrExpXeSoi.length; j++) {
                int expXS = random.nextInt(num);
                arrExpXeSoi[j] = expXS;
            }
            for (int a = 0; a < arrExpXeSoi.length; a++) {
                int b = random.nextInt(expXeSoi.length);
                if (i == arrExpXeSoi[a]) {
                    result[i] = (short) expXeSoi[b];
                }
            }
            //rd ttts
            for (int j = 0; j < arrTTTS.length; j++) {
                int ttts = random.nextInt(num);
                arrTTTS[j] = ttts;
            }
            for (int a = 0; a < arrTTTS.length; a++) {
                if (i == arrTTTS[a]) {
                    result[i] = (short) Integer.parseInt("455");
                }
            }
            //rd tttt
            for (int j = 0; j < arrTTTT.length; j++) {
                int tttt = random.nextInt(num);
                arrTTTT[j] = tttt;
            }
            for (int a = 0; a < arrTTTT.length; a++) {
                if (i == arrTTTT[a]) {
                    result[i] = (short) Integer.parseInt("456");
                }
            }

            //rd dá 7
            for (int j = 0; j < arrDaMaThuat.length; j++) {
                int dmt = random.nextInt(num);
                arrDaMaThuat[j] = dmt;
            }
            for (int a = 0; a < arrDaMaThuat.length; a++) {
                if (i == arrDaMaThuat[a]) {
                    result[i] = (short) Integer.parseInt("6");
                }
            }
            //rd ptl
            for (int j = 0; j < arrPtl.length; j++) {
                int ptl = random.nextInt(num);
                arrPtl[j] = ptl;
            }
            for (int a = 0; a < arrPtl.length; a++) {
                if (i == arrPtl[a]) {
                    result[i] = (short) Integer.parseInt("545");
                }
            }
            //rd ctt
            for (int j = 0; j < arrCTT.length; j++) {
                int ctt = random.nextInt(num);
                arrCTT[j] = ctt;
            }
            for (int a = 0; a < arrCTT.length; a++) {
                if (i == arrCTT[a]) {
                    result[i] = (short) Integer.parseInt("454");
                }
            }

            //rd tttc
//            for (int j=0;j<co.length;j++){
//                int tttc=random.nextInt(num);
//                co[j]=tttc;
//            }
//            for (int a=0;a<co.length;a++){
//                if (i==co[a]){
//                    result[i]= (short) Integer.parseInt("778");
//                }
//            }
            //rd trang bị xe soi
            for (int j = 0; j < arrtbx.length; j++) {
                int tb = random.nextInt(num);
                arrtbx[j] = tb;
            }
            for (int a = 0; a < arrtbx.length; a++) {
                int b = random.nextInt(tbXeSoi.length);
                if (i == arrtbx[a]) {
                    result[i] = (short) tbXeSoi[b];
                }
            }

            //rd exp soi 2x
            for (int j = 0; j < arrExpXeSoi2X.length; j++) {
                int expXS = random.nextInt(num);
                arrExpXeSoi2X[j] = expXS;
            }
            for (int a = 0; a < arrExpXeSoi2X.length; a++) {
                int b = random.nextInt(expSoi2x.length);
                if (i == arrExpXeSoi2X[a]) {
                    result[i] = (short) expSoi2x[b];
                }
            }
        }
        return result;
    }

    private short[] getItemVDMQ(short num) {
        short[] result = new short[num];
        Random random = new Random();
        int[] arrCTT = new int[1];
        int[] arrYen = new int[3000];
        int[] arrTTTS = new int[5];
        int[] arrTTTT = new int[1];

        int[] co = new int[150];
        int[] arrtbx = new int[8];
        int[] arrExpXeSoi = new int[20];
        int[] arrExpXeSoi2X = new int[500];
        int[] arrDaMaThuat = new int[50];
        int[] arrPtl = new int[2];
        int[] tbXeSoi = {439, 440, 441, 442, 486, 487, 488, 489, 439, 440, 441, 442, 486, 487, 488, 489};
        int[] expXeSoi = {573, 574, 575, 576, 577, 578, 573, 574, 575, 576, 577, 578};
        //da 5 6 7 
        int[] expSoi2x = {4, 5, 6, 0};
        for (int i = 0; i < num; i++) {
            result[i] = (short) -1;
            //rd yen
            for (int j = 0; j < arrYen.length; j++) {
                int yen = random.nextInt(num);
                arrYen[j] = yen;
            }
            for (int a = 0; a < arrYen.length; a++) {
                if (i == arrYen[a]) {
                    result[i] = (short) Integer.parseInt("12");
                }
            }
            //rd exp xe soi
            for (int j = 0; j < arrExpXeSoi.length; j++) {
                int expXS = random.nextInt(num);
                arrExpXeSoi[j] = expXS;
            }
            for (int a = 0; a < arrExpXeSoi.length; a++) {
                int b = random.nextInt(expXeSoi.length);
                if (i == arrExpXeSoi[a]) {
                    result[i] = (short) expXeSoi[b];
                }
            }
            //rd ttts
            for (int j = 0; j < arrTTTS.length; j++) {
                int ttts = random.nextInt(num);
                arrTTTS[j] = ttts;
            }
            for (int a = 0; a < arrTTTS.length; a++) {
                if (i == arrTTTS[a]) {
                    result[i] = (short) Integer.parseInt("455");
                }
            }
            //rd tttt
            for (int j = 0; j < arrTTTT.length; j++) {
                int tttt = random.nextInt(num);
                arrTTTT[j] = tttt;
            }
            for (int a = 0; a < arrTTTT.length; a++) {
                if (i == arrTTTT[a]) {
                    result[i] = (short) Integer.parseInt("456");
                }
            }

            //rd dá 7
            for (int j = 0; j < arrDaMaThuat.length; j++) {
                int dmt = random.nextInt(num);
                arrDaMaThuat[j] = dmt;
            }
            for (int a = 0; a < arrDaMaThuat.length; a++) {
                if (i == arrDaMaThuat[a]) {
                    result[i] = (short) Integer.parseInt("6");
                }
            }
            //rd ptl
            for (int j = 0; j < arrPtl.length; j++) {
                int ptl = random.nextInt(num);
                arrPtl[j] = ptl;
            }
            for (int a = 0; a < arrPtl.length; a++) {
                if (i == arrPtl[a]) {
                    result[i] = (short) Integer.parseInt("545");
                }
            }
            //rd ctt
            for (int j = 0; j < arrCTT.length; j++) {
                int ctt = random.nextInt(num);
                arrCTT[j] = ctt;
            }
            for (int a = 0; a < arrCTT.length; a++) {
                if (i == arrCTT[a]) {
                    result[i] = (short) Integer.parseInt("454");
                }
            }

            //rd tttc
            for (int j = 0; j < co.length; j++) {
                int tttc = random.nextInt(num);
                co[j] = tttc;
            }
            for (int a = 0; a < co.length; a++) {
                if (i == co[a]) {
                    result[i] = (short) Integer.parseInt("778");
                }
            }
            //rd trang bị xe soi
            for (int j = 0; j < arrtbx.length; j++) {
                int tb = random.nextInt(num);
                arrtbx[j] = tb;
            }
            for (int a = 0; a < arrtbx.length; a++) {
                int b = random.nextInt(tbXeSoi.length);
                if (i == arrtbx[a]) {
                    result[i] = (short) tbXeSoi[b];
                }
            }

            //rd exp soi 2x
            for (int j = 0; j < arrExpXeSoi2X.length; j++) {
                int expXS = random.nextInt(num);
                arrExpXeSoi2X[j] = expXS;
            }
            for (int a = 0; a < arrExpXeSoi2X.length; a++) {
                int b = random.nextInt(expSoi2x.length);
                if (i == arrExpXeSoi2X[a]) {
                    result[i] = (short) expSoi2x[b];
                }
            }
        }
        return result;
    }

    private short[] getItemShinba1(short num) {
        short[] result = new short[num];
        Random random = new Random();
        int[] arrCTT = new int[2];
        int[] arrBima = new int[2000];
        int[] arrMS = new int[2000];

        int[] arrYen = new int[2000];
        int[] arrTTTS = new int[15];
        int[] arrTTTT = new int[5];
        int[] arrTTTC = new int[1];
        int[] co = new int[200];
        int[] arrtbx = new int[8];
        int[] arrExpXeSoi = new int[40];
        int[] arrExpXeSoi2X = new int[150];
        int[] arrDaMaThuat = new int[50];
        int[] arrPtl = new int[3];
        int[] arrDDV = new int[10];
        int[] arrCT = new int[60];
        int[] listCT = {775, 788, 789, 0};
        int[] tbXeSoi = {439, 440, 441, 442, 486, 487, 488, 489};
        int[] expXeSoi = {573, 574, 575, 576, 577, 578};
        //da 5 6 7 
        int[] expSoi2x = {4, 5, 6, 0};
        for (int i = 0; i < num; i++) {
            result[i] = (short) -1;
            //rd yen
            for (int j = 0; j < arrYen.length; j++) {
                int yen = random.nextInt(num);
                arrYen[j] = yen;
            }
            for (int a = 0; a < arrYen.length; a++) {
                if (i == arrYen[a]) {
                    result[i] = (short) Integer.parseInt("12");
                }
            }
            for (int j = 0; j < arrDDV.length; j++) {
                int ddv = random.nextInt(num);
                arrDDV[j] = ddv;
            }
            for (int a = 0; a < arrDDV.length; a++) {
                if (i == arrDDV[a]) {
                    result[i] = (short) Integer.parseInt("695");
                }
            }
            //rd exp xe soi
            for (int j = 0; j < arrExpXeSoi.length; j++) {
                int expXS = random.nextInt(num);
                arrExpXeSoi[j] = expXS;
            }
            for (int a = 0; a < arrExpXeSoi.length; a++) {
                int b = random.nextInt(expXeSoi.length);
                if (i == arrExpXeSoi[a]) {
                    result[i] = (short) expXeSoi[b];
                }
            }
            //rd cai trang
            for (int j = 0; j < arrCT.length; j++) {
                int ct = random.nextInt(num);
                arrCT[j] = ct;
            }
            for (int a = 0; a < arrCT.length; a++) {
                int b = random.nextInt(listCT.length);
                if (i == arrCT[a]) {
                    result[i] = (short) listCT[b];
                }
            }
            //rd ttts
            for (int j = 0; j < arrTTTS.length; j++) {
                int ttts = random.nextInt(num);
                arrTTTS[j] = ttts;
            }
            for (int a = 0; a < arrTTTS.length; a++) {
                if (i == arrTTTS[a]) {
                    result[i] = (short) Integer.parseInt("455");
                }
            }
            //rd tttt
            for (int j = 0; j < arrTTTT.length; j++) {
                int tttt = random.nextInt(num);
                arrTTTT[j] = tttt;
            }
            for (int a = 0; a < arrTTTT.length; a++) {
                if (i == arrTTTT[a]) {
                    result[i] = (short) Integer.parseInt("456");
                }
            }

            for (int j = 0; j < arrTTTC.length; j++) {
                int tttc = random.nextInt(num);
                arrTTTC[j] = tttc;
            }
            for (int a = 0; a < arrTTTC.length; a++) {
                if (i == arrTTTC[a]) {
                    result[i] = (short) Integer.parseInt("455");
                }
            }

            //rd dá 7
            for (int j = 0; j < arrDaMaThuat.length; j++) {
                int dmt = random.nextInt(num);
                arrDaMaThuat[j] = dmt;
            }
            for (int a = 0; a < arrDaMaThuat.length; a++) {
                if (i == arrDaMaThuat[a]) {
                    result[i] = (short) Integer.parseInt("6");
                }
            }
            //rd ptl
            for (int j = 0; j < arrPtl.length; j++) {
                int ptl = random.nextInt(num);
                arrPtl[j] = ptl;
            }
            for (int a = 0; a < arrPtl.length; a++) {
                if (i == arrPtl[a]) {
                    result[i] = (short) Integer.parseInt("545");
                }
            }
            //rd ctt
            for (int j = 0; j < arrCTT.length; j++) {
                int ctt = random.nextInt(num);
                arrCTT[j] = ctt;
            }
            for (int a = 0; a < arrCTT.length; a++) {
                if (i == arrCTT[a]) {
                    result[i] = (short) Integer.parseInt("454");
                }
            }
            //bí ma
            for (int j = 0; j < arrBima.length; j++) {
                int bima = random.nextInt(num);
                arrBima[j] = bima;
            }
            for (int a = 0; a < arrBima.length; a++) {
                if (i == arrBima[a]) {
                    result[i] = (short) Integer.parseInt("983");
                }
            }
            //ms
            for (int j = 0; j < arrMS.length; j++) {
                int ms = random.nextInt(num);
                arrMS[j] = ms;
            }
            for (int a = 0; a < arrMS.length; a++) {
                if (i == arrMS[a]) {
                    result[i] = (short) Integer.parseInt("991");
                }
            }
            //rd co
            for (int j = 0; j < co.length; j++) {
                int tttc = random.nextInt(num);
                co[j] = tttc;
            }
            for (int a = 0; a < co.length; a++) {
                if (i == co[a]) {
                    result[i] = (short) Integer.parseInt("778");
                }
            }
            //rd trang bị xe soi
            for (int j = 0; j < arrtbx.length; j++) {
                int tb = random.nextInt(num);
                arrtbx[j] = tb;
            }
            for (int a = 0; a < arrtbx.length; a++) {
                int b = random.nextInt(tbXeSoi.length);
                if (i == arrtbx[a]) {
                    result[i] = (short) tbXeSoi[b];
                }
            }

            //rd exp soi 2x
            for (int j = 0; j < arrExpXeSoi2X.length; j++) {
                int expXS = random.nextInt(num);
                arrExpXeSoi2X[j] = expXS;
            }
            for (int a = 0; a < arrExpXeSoi2X.length; a++) {
                int b = random.nextInt(expSoi2x.length);
                if (i == arrExpXeSoi2X[a]) {
                    result[i] = (short) expSoi2x[b];
                }
            }
        }
        return result;
    }

    private short[] getItemLC(short num) {
        short[] result = new short[num];
        Random random = new Random();
        int[] arrCTT = new int[2];
        int[] arrYen = new int[2000];
        int[] arrTTTS = new int[15];
        int[] arrTTTT = new int[5];
        int[] arrTTTC = new int[1];
        int[] co = new int[200];
        int[] arrtbx = new int[8];
        int[] arrExpXeSoi = new int[40];
        int[] arrExpXeSoi2X = new int[150];
        int[] arrDaMaThuat = new int[50];
        int[] arrPtl = new int[3];
        int[] arrDDV = new int[10];
        int[] arrCT = new int[60];
        int[] listCT = {775, 788, 789, 0};
        int[] tbXeSoi = {439, 440, 441, 442, 486, 487, 488, 489};
        int[] expXeSoi = {573, 574, 575, 576, 577, 578};
        //da 5 6 7 
        int[] expSoi2x = {4, 5, 6, 0};
        for (int i = 0; i < num; i++) {
            result[i] = (short) -1;
            //rd yen
            for (int j = 0; j < arrYen.length; j++) {
                int yen = random.nextInt(num);
                arrYen[j] = yen;
            }
            for (int a = 0; a < arrYen.length; a++) {
                if (i == arrYen[a]) {
                    result[i] = (short) Integer.parseInt("12");
                }
            }
            for (int j = 0; j < arrDDV.length; j++) {
                int ddv = random.nextInt(num);
                arrDDV[j] = ddv;
            }
            for (int a = 0; a < arrDDV.length; a++) {
                if (i == arrDDV[a]) {
                    result[i] = (short) Integer.parseInt("695");
                }
            }
            //rd exp xe soi
            for (int j = 0; j < arrExpXeSoi.length; j++) {
                int expXS = random.nextInt(num);
                arrExpXeSoi[j] = expXS;
            }
            for (int a = 0; a < arrExpXeSoi.length; a++) {
                int b = random.nextInt(expXeSoi.length);
                if (i == arrExpXeSoi[a]) {
                    result[i] = (short) expXeSoi[b];
                }
            }
            //rd cai trang
            for (int j = 0; j < arrCT.length; j++) {
                int ct = random.nextInt(num);
                arrCT[j] = ct;
            }
            for (int a = 0; a < arrCT.length; a++) {
                int b = random.nextInt(listCT.length);
                if (i == arrCT[a]) {
                    result[i] = (short) listCT[b];
                }
            }
            //rd ttts
            for (int j = 0; j < arrTTTS.length; j++) {
                int ttts = random.nextInt(num);
                arrTTTS[j] = ttts;
            }
            for (int a = 0; a < arrTTTS.length; a++) {
                if (i == arrTTTS[a]) {
                    result[i] = (short) Integer.parseInt("455");
                }
            }
            //rd tttt
            for (int j = 0; j < arrTTTT.length; j++) {
                int tttt = random.nextInt(num);
                arrTTTT[j] = tttt;
            }
            for (int a = 0; a < arrTTTT.length; a++) {
                if (i == arrTTTT[a]) {
                    result[i] = (short) Integer.parseInt("456");
                }
            }

            for (int j = 0; j < arrTTTC.length; j++) {
                int tttc = random.nextInt(num);
                arrTTTC[j] = tttc;
            }
            for (int a = 0; a < arrTTTC.length; a++) {
                if (i == arrTTTC[a]) {
                    result[i] = (short) Integer.parseInt("455");
                }
            }

            //rd dá 7
            for (int j = 0; j < arrDaMaThuat.length; j++) {
                int dmt = random.nextInt(num);
                arrDaMaThuat[j] = dmt;
            }
            for (int a = 0; a < arrDaMaThuat.length; a++) {
                if (i == arrDaMaThuat[a]) {
                    result[i] = (short) Integer.parseInt("6");
                }
            }
            //rd ptl
            for (int j = 0; j < arrPtl.length; j++) {
                int ptl = random.nextInt(num);
                arrPtl[j] = ptl;
            }
            for (int a = 0; a < arrPtl.length; a++) {
                if (i == arrPtl[a]) {
                    result[i] = (short) Integer.parseInt("545");
                }
            }
            //rd ctt
            for (int j = 0; j < arrCTT.length; j++) {
                int ctt = random.nextInt(num);
                arrCTT[j] = ctt;
            }
            for (int a = 0; a < arrCTT.length; a++) {
                if (i == arrCTT[a]) {
                    result[i] = (short) Integer.parseInt("454");
                }
            }

            //rd co
            for (int j = 0; j < co.length; j++) {
                int tttc = random.nextInt(num);
                co[j] = tttc;
            }
            for (int a = 0; a < co.length; a++) {
                if (i == co[a]) {
                    result[i] = (short) Integer.parseInt("778");
                }
            }
            //rd trang bị xe soi
            for (int j = 0; j < arrtbx.length; j++) {
                int tb = random.nextInt(num);
                arrtbx[j] = tb;
            }
            for (int a = 0; a < arrtbx.length; a++) {
                int b = random.nextInt(tbXeSoi.length);
                if (i == arrtbx[a]) {
                    result[i] = (short) tbXeSoi[b];
                }
            }

            //rd exp soi 2x
            for (int j = 0; j < arrExpXeSoi2X.length; j++) {
                int expXS = random.nextInt(num);
                arrExpXeSoi2X[j] = expXS;
            }
            for (int a = 0; a < arrExpXeSoi2X.length; a++) {
                int b = random.nextInt(expSoi2x.length);
                if (i == arrExpXeSoi2X[a]) {
                    result[i] = (short) expSoi2x[b];
                }
            }
        }
        return result;
    }

    private String[] parseStringArray(String token) {
        return token.split("\\,\\s*?");
    }

    private short[] parseShortArray(String input) {
        val tokens = input.split("\\,\\s*?");
        val result = new short[tokens.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Short.parseShort(tokens[i].trim());
        }
        return result;
    }

    private int[] parseIntArray(String input) {
        val tokens = input.split("\\,\\s*?");
        val result = new int[tokens.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Integer.parseInt(tokens[i].trim());
        }
        return result;
    }

    public static Map getMapid(final int id) {
        synchronized (Manager.server.getMaps()) {
            for (short i = 0; i < Manager.server.getMaps().length; ++i) {
                final Map map = Manager.server.getMapById(i);
                if (map != null && map.id == id) {
                    return map;
                }
            }
            return null;
        }
    }

    @SneakyThrows
    private void loadConfigFile() {
        final byte[] ab = GameScr.loadFile("ninja.conf").toByteArray();
        if (ab == null) {
            util.Debug("Config file not found!");
            System.exit(0);
        }
        final String data = new String(ab);
        final HashMap<String, String> configMap = new HashMap<String, String>();
        final StringBuilder sbd = new StringBuilder();
        boolean bo = false;
        for (int i = 0; i <= data.length(); ++i) {
            final char es;
            if (i == data.length() || (es = data.charAt(i)) == '\n') {
                bo = false;
                final String sbf = sbd.toString().trim();
                if (sbf != null && !sbf.equals("") && sbf.charAt(0) != '#') {
                    final int j = sbf.indexOf(58);
                    if (j > 0) {
                        final String key = sbf.substring(0, j).trim();
                        final String value = sbf.substring(j + 1).trim();
                        configMap.put(key, value);
                        util.Debug("config: " + key + "-" + value);
                    }
                }
                sbd.setLength(0);
            } else {
                if (es == '#') {
                    bo = true;
                }
                if (!bo) {
                    sbd.append(es);
                }
            }
        }
        if (configMap.containsKey("debug")) {
            util.setDebug(Boolean.parseBoolean(configMap.get("debug")));
        } else {
            util.setDebug(false);
        }

        if (configMap.containsKey("host")) {
            this.host = configMap.get("host");
        } else {
            this.host = "localhost";
        }
        if (configMap.containsKey("post")) {
            this.PORT = Short.parseShort(configMap.get("post"));
        } else {
            this.PORT = 14444;
        }
        if (configMap.containsKey("mysql-host")) {
            this.mysql_host = configMap.get("mysql-host");
        } else {
            this.mysql_host = "localhost";
        }
        if (configMap.containsKey("mysql-user")) {
            this.mysql_user = configMap.get("mysql-user");
        } else {
            this.mysql_user = "root";
        }
        if (configMap.containsKey("mysql-password")) {
            this.mysql_pass = configMap.get("mysql-password");
        } else {
            this.mysql_pass = "";
        }
        if (configMap.containsKey("mysql-database")) {
            this.mysql_database = configMap.get("mysql-database");
        } else {
            this.mysql_database = "ninja";
        }
        if (configMap.containsKey("version-Data")) {
            this.vsData = Byte.parseByte(configMap.get("version-Data"));
        } else {
            this.vsData = 54;
        }
        if (configMap.containsKey("version-Map")) {
            this.vsMap = Byte.parseByte(configMap.get("version-Map"));
        } else {
            this.vsMap = 86;
        }
        if (configMap.containsKey("version-Skill")) {
            this.vsSkill = Byte.parseByte(configMap.get("version-Skill"));
        } else {
            this.vsSkill = 10;
        }
        if (configMap.containsKey("version-Item")) {
            this.vsItem = Byte.parseByte(configMap.get("version-Item"));
        } else {
            this.vsItem = 70;
        }

        MAX_CLIENT_PER_SOCKET = Short.parseShort(configMap.get("max-Client-Per-Socket"));
        SQLManager.create(host, mysql_database, mysql_user, mysql_pass);

    }

    public void loadCache() {
        final int[] i = {0};
        try {
            System.out.println("Load map cache...");
            SQLManager.executeQuery("SELECT * FROM `map`;", (res) -> {
                if (res.last()) {
                    mapCache = new MapCache[res.getRow()];
                    res.beforeFirst();
                }
                i[0] = 0;
                while (res.next()) {
                    final MapCache mapTemplate = new MapCache();
                    mapTemplate.mapName = res.getString("name");
                    mapCache[i[0]] = mapTemplate;
                    ++i[0];
                }
                res.close();
            });
            SQLManager.executeQuery("SELECT * FROM `npc`;", (res) -> {
                if (res.last()) {
                    npcCache = new NpcCache[res.getRow()];
                    res.beforeFirst();
                }
                i[0] = 0;
                while (res.next()) {
                    final NpcCache npcTemplate = new NpcCache();
                    npcTemplate.name = res.getString("name");
                    npcTemplate.headId = res.getShort("head");
                    npcTemplate.bodyId = res.getShort("body");
                    npcTemplate.legId = res.getShort("leg");
                    final JSONArray jarr = (JSONArray) JSONValue.parse(res.getString("talk"));
                    npcTemplate.menu = new String[jarr.size()][];
                    for (int j = 0; j < npcTemplate.menu.length; ++j) {
                        final JSONArray jarr2 = (JSONArray) jarr.get(j);
                        npcTemplate.menu[j] = new String[jarr2.size()];
                        for (int k2 = 0; k2 < npcTemplate.menu[j].length; ++k2) {
                            npcTemplate.menu[j][k2] = jarr2.get(k2).toString();
                        }
                    }
                    npcCache[i[0]] = npcTemplate;
                    ++i[0];
                }
                res.close();
            });
            SQLManager.executeQuery("SELECT * FROM `mob`;", (res) -> {
                if (res.last()) {
                    mobCache = new MobCache[res.getRow()];
                    res.beforeFirst();
                }
                i[0] = 0;
                while (res.next()) {
                    final MobCache mobTemplate = new MobCache();
                    mobTemplate.type = res.getByte("type");
                    mobTemplate.name = res.getString("name");
                    mobTemplate.hp = res.getInt("hp");
                    mobTemplate.rangeMove = res.getByte("rangeMove");
                    mobTemplate.speed = res.getByte("speed");
                    mobCache[i[0]] = mobTemplate;
                    ++i[0];
                }
                res.close();
            });
            System.out.println("Load item cache...");

            SQLManager.executeQuery("SELECT * FROM `optionitem`;", (res) -> {
                if (res.last()) {
                    iOptionTemplates = new ItemOptionTemplate[res.getRow()];
                    res.beforeFirst();
                }
                int j = 0;
                while (res.next()) {
                    final ItemOptionTemplate iotemplate = new ItemOptionTemplate();
                    iotemplate.id = res.getInt("id");
                    iotemplate.name = res.getString("name");
                    iotemplate.type = res.getByte("type");
                    iOptionTemplates[j] = iotemplate;
                    System.out.println("iotemplate:"+iotemplate.toString());
                    ++j;
                }
                res.close();
            });
            SQLManager.executeQuery("SELECT * FROM `item`;", (res) -> {
                if (res.last()) {
                    itemTemplates = new ItemCache[res.getRow()];
                    res.beforeFirst();
                }
                i[0] = 0;
                while (res.next()) {
                    final ItemCache itemTemplate = new ItemCache();
                    itemTemplate.id = res.getShort("id");
                    itemTemplate.type = res.getByte("type");
                    itemTemplate.gender = res.getByte("gender");
                    itemTemplate.name = res.getString("name");
                    itemTemplate.description = res.getString("description");
                    itemTemplate.level = res.getInt("level");
                    itemTemplate.iconID = res.getShort("iconID");
                    itemTemplate.part = res.getShort("part");
                    itemTemplate.isUpToUp = res.getBoolean("uptoup");
                    itemTemplates[i[0]] = itemTemplate;
                    ++i[0];
                }

                res.close();
            });
        } catch (Exception e) {
            util.Debug("Error i:" + i[0]);
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void loadSkillFromData() {
        System.out.println("bat dau loaddatabase");
        SQLManager.create1(this.mysql_host, this.mysql_database, this.mysql_user, this.mysql_pass);
        int i = 0;
        try {
            ResultSet res = SQLManager.stat.executeQuery("SELECT * FROM `nj_arrow`");
            while (res.next()) {
                ArrowPaint p = new ArrowPaint();
                p.id = res.getShort("id");
                JSONArray jA = (JSONArray) JSONValue.parse(res.getString("imgId"));
                p.imgId[0] = ((Long) jA.get(0)).shortValue();
                p.imgId[1] = ((Long) jA.get(1)).shortValue();
                p.imgId[2] = ((Long) jA.get(2)).shortValue();
                arrs.add(p);
            }
            res.close();
            i = 0;
            res = SQLManager.stat.executeQuery("SELECT * FROM `nj_image`;");
            while (res.next()) {
                int[] smallImage = new int[5];
                JSONArray jA = (JSONArray) JSONValue.parse(res.getString("smallImage"));
                smallImage[0] = ((Long) jA.get(0)).intValue();
                smallImage[1] = ((Long) jA.get(1)).intValue();
                smallImage[2] = ((Long) jA.get(2)).intValue();
                smallImage[3] = ((Long) jA.get(3)).intValue();
                smallImage[4] = ((Long) jA.get(4)).intValue();
                smallImg.add(smallImage);
            }
            res.close();
            i = 0;
            res = SQLManager.stat.executeQuery("SELECT * FROM `nj_effect`;");
            while (res.next()) {
                EffectCharPaint effectCharInfo = new EffectCharPaint();
                effectCharInfo.idEf = res.getShort("id");
                JSONArray jA = (JSONArray) JSONValue.parse(res.getString("info"));
                effectCharInfo.arrEfInfo = new EffectInfoPaint[jA.size()];
                for (int k = 0; k < effectCharInfo.arrEfInfo.length; k++) {
                    JSONObject o = (JSONObject) jA.get(k);
                    effectCharInfo.arrEfInfo[k] = new EffectInfoPaint();
                    effectCharInfo.arrEfInfo[k].idImg = ((Long) o.get("imgId")).shortValue();
                    effectCharInfo.arrEfInfo[k].dx = ((Long) o.get("dx")).byteValue();
                    effectCharInfo.arrEfInfo[k].dy = ((Long) o.get("dy")).byteValue();
                }
                efs.add(effectCharInfo);
            }
            res.close();
            i = 0;
            res = SQLManager.stat.executeQuery("SELECT * FROM `nj_skill`;");
            while (res.next()) {
                SkillPaint p = new SkillPaint();
                p.id = res.getShort("skillId");
                p.effId = res.getShort("effId");
                p.numEff = res.getByte("numEff");
                JSONArray jA = (JSONArray) JSONValue.parse(res.getString("skillStand"));
                p.skillStand = new SkillInfoPaint[jA.size()];
                for (int k = 0; k < p.skillStand.length; k++) {
                    JSONObject o = (JSONObject) jA.get(k);
                    p.skillStand[k] = new SkillInfoPaint();
                    p.skillStand[k].status = ((Long) o.get("status")).byteValue();
                    p.skillStand[k].effS0Id = ((Long) o.get("effS0Id")).shortValue();
                    p.skillStand[k].e0dx = ((Long) o.get("e0dx")).shortValue();
                    p.skillStand[k].e0dy = ((Long) o.get("e0dy")).shortValue();
                    p.skillStand[k].effS1Id = ((Long) o.get("effS1Id")).shortValue();
                    p.skillStand[k].e1dx = ((Long) o.get("e1dx")).shortValue();
                    p.skillStand[k].e1dy = ((Long) o.get("e1dy")).shortValue();
                    p.skillStand[k].effS2Id = ((Long) o.get("effS2Id")).shortValue();
                    p.skillStand[k].e2dx = ((Long) o.get("e2dx")).shortValue();
                    p.skillStand[k].e2dy = ((Long) o.get("e2dy")).shortValue();
                    p.skillStand[k].arrowId = ((Long) o.get("arrowId")).shortValue();
                    p.skillStand[k].adx = ((Long) o.get("adx")).shortValue();
                    p.skillStand[k].ady = ((Long) o.get("ady")).shortValue();
                }
                jA = (JSONArray) JSONValue.parse(res.getString("skillFly"));
                p.skillfly = new SkillInfoPaint[jA.size()];
                for (int k = 0; k < p.skillfly.length; k++) {
                    JSONObject o = (JSONObject) jA.get(k);
                    p.skillfly[k] = new SkillInfoPaint();
                    p.skillfly[k].status = ((Long) o.get("status")).byteValue();
                    p.skillfly[k].effS0Id = ((Long) o.get("effS0Id")).shortValue();
                    p.skillfly[k].e0dx = ((Long) o.get("e0dx")).shortValue();
                    p.skillfly[k].e0dy = ((Long) o.get("e0dy")).shortValue();
                    p.skillfly[k].effS1Id = ((Long) o.get("effS1Id")).shortValue();
                    p.skillfly[k].e1dx = ((Long) o.get("e1dx")).shortValue();
                    p.skillfly[k].e1dy = ((Long) o.get("e1dy")).shortValue();
                    p.skillfly[k].effS2Id = ((Long) o.get("effS2Id")).shortValue();
                    p.skillfly[k].e2dx = ((Long) o.get("e2dx")).shortValue();
                    p.skillfly[k].e2dy = ((Long) o.get("e2dy")).shortValue();
                    p.skillfly[k].arrowId = ((Long) o.get("arrowId")).shortValue();
                    p.skillfly[k].adx = ((Long) o.get("adx")).shortValue();
                    p.skillfly[k].ady = ((Long) o.get("ady")).shortValue();
                }
                sks.add(p);
            }
            res.close();
            i = 0;
            res = SQLManager.stat.executeQuery("SELECT * FROM `nj_part`;");
            while (res.next()) {
                byte type = res.getByte("type");
                JSONArray jA = (JSONArray) JSONValue.parse(res.getString("part"));
                Part part = new Part(type);
                for (i = 0; i < part.pi.length; i++) {
                    JSONObject o = (JSONObject) jA.get(i);
                    part.pi[i] = new PartImage();
                    part.pi[i].id = ((Long) o.get("id")).shortValue();
                    part.pi[i].dx = ((Long) o.get("dx")).byteValue();
                    part.pi[i].dy = ((Long) o.get("dy")).byteValue();
                }
                parts.add(part);
            }
            res.close();
            try {
                res = SQLManager.stat.executeQuery("SELECT * FROM `optionskill`;");
                if (res.last()) {
                    sOptionTemplates = new SkillOptionTemplates[res.getRow()];
                    res.beforeFirst();
                }
                i = 0;
                while (res.next()) {
                    final SkillOptionTemplates sotemplate = new SkillOptionTemplates();
                    sotemplate.id = res.getInt("id");
                    sotemplate.name = res.getString("name");
                    sOptionTemplates[i] = sotemplate;
                    ++i;
                }
                res.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        } catch (Exception e) {
            System.out.println("Error i:" + i);
            e.printStackTrace();
            System.exit(0);
        }
        try {
            SQLManager.stat.close();
        } catch (SQLException ex) {
            Logger.getLogger(Manager.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        this.setCache(0);
this.setCache(1);
    }

    private void setCache(int id) {
        System.out.println("bat dau setcache");
        try {
            ByteArrayOutputStream bas;
            DataOutputStream dos;
            switch (id) {
                case 0: {
                    bas = new ByteArrayOutputStream();
                    dos = new DataOutputStream(bas);
                    dos.writeShort(Manager.arrs.size());
                    for (ArrowPaint arr : Manager.arrs) {
                        dos.writeShort(arr.id);
                        dos.writeShort(arr.imgId[0]);
                        dos.writeShort(arr.imgId[1]);
                        dos.writeShort(arr.imgId[2]);
                    }
                    dos.flush();
                    byte[] ab = bas.toByteArray();
                    GameScr.saveFile("res/cache/data/nj_arrow", ab);
                    bas = new ByteArrayOutputStream();
                    dos = new DataOutputStream(bas);
                    dos.writeShort(Manager.smallImg.size());
                    for (int[] img : Manager.smallImg) {
                        dos.writeByte(img[0]);
                        dos.writeShort(img[1]);
                        dos.writeShort(img[2]);
                        dos.writeShort(img[3]);
                        dos.writeShort(img[4]);
                    }
                    dos.flush();
                    ab = bas.toByteArray();
                    GameScr.saveFile("res/cache/data/nj_image", ab);
                    bas = new ByteArrayOutputStream();
                    dos = new DataOutputStream(bas);
                    dos.writeShort(Manager.efs.size());
                    for (EffectCharPaint eff : Manager.efs) {
                        dos.writeShort(eff.idEf);
                        dos.writeByte(eff.arrEfInfo.length);
                        for (EffectInfoPaint eff2 : eff.arrEfInfo) {
                            dos.writeShort(eff2.idImg);
                            dos.writeByte(eff2.dx);
                            dos.writeByte(eff2.dy);
                        }
                    }
                    dos.flush();
                    ab = bas.toByteArray();
                    GameScr.saveFile("res/cache/data/nj_effect", ab);
                    bas = new ByteArrayOutputStream();
                    dos = new DataOutputStream(bas);
                    dos.writeShort(Manager.sks.size());
                    for (SkillPaint p : Manager.sks) {
                        dos.writeShort(p.id);
                        dos.writeShort(p.effId);
                        dos.writeByte(p.numEff);
                        dos.writeByte(p.skillStand.length);
                        for (SkillInfoPaint skillStand : p.skillStand) {
                            dos.writeByte(skillStand.status);
                            dos.writeShort(skillStand.effS0Id);
                            dos.writeShort(skillStand.e0dx);
                            dos.writeShort(skillStand.e0dy);
                            dos.writeShort(skillStand.effS1Id);
                            dos.writeShort(skillStand.e1dx);
                            dos.writeShort(skillStand.e1dy);
                            dos.writeShort(skillStand.effS2Id);
                            dos.writeShort(skillStand.e2dx);
                            dos.writeShort(skillStand.e2dy);
                            dos.writeShort(skillStand.arrowId);
                            dos.writeShort(skillStand.adx);
                            dos.writeShort(skillStand.ady);
                        }
                        dos.writeByte(p.skillfly.length);
                        for (SkillInfoPaint skillfly : p.skillfly) {
                            dos.writeByte(skillfly.status);
                            dos.writeShort(skillfly.effS0Id);
                            dos.writeShort(skillfly.e0dx);
                            dos.writeShort(skillfly.e0dy);
                            dos.writeShort(skillfly.effS1Id);
                            dos.writeShort(skillfly.e1dx);
                            dos.writeShort(skillfly.e1dy);
                            dos.writeShort(skillfly.effS2Id);
                            dos.writeShort(skillfly.e2dx);
                            dos.writeShort(skillfly.e2dy);
                            dos.writeShort(skillfly.arrowId);
                            dos.writeShort(skillfly.adx);
                            dos.writeShort(skillfly.ady);
                        }
                    }
                    dos.flush();
                    ab = bas.toByteArray();
                    GameScr.saveFile("res/cache/data/nj_skill", ab);
                    dos.close();
                    bas.close();
                    break;
                }
                case 1: {
                    System.out.println("Chay qua day!!!!!!!!!");
                    bas = new ByteArrayOutputStream();
                    dos = new DataOutputStream(bas);
                    dos.writeShort(Manager.parts.size());
                    for (Part p : Manager.parts) {
                        dos.writeByte(p.type);
                        for (PartImage pi : p.pi) {
                            dos.writeShort(pi.id);
                            dos.writeByte(pi.dx);
                            dos.writeByte(pi.dy);
                        }
                    }
                    byte[] ab = bas.toByteArray();
                    System.out.println("Lưu res/cache/part");
                     //GameScr.saveFile("res/cache/data/nj_part", ab);
                    GameScr.saveFile("cache/part", ab);
                    dos.close();
                    bas.close();
                    break;
                }
            }
        } catch (IOException var15) {
            var15.printStackTrace();
        }

    }

    public void loadDataBase() {
        entrys.clear();
        ItemSell.entrys.clear();

        final int[] i = {0};
        try {

            /**
             * OK
             */
            SQLManager.executeQuery("SELECT * FROM `tasks`;", (res) -> {

                if (res.last()) {
                    this.tasks = new byte[res.getRow()][];
                    this.maptasks = new byte[this.tasks.length][];
                    res.beforeFirst();
                }
                while (res.next()) {
                    final JSONArray jarr = (JSONArray) JSONValue.parse(res.getString("tasks"));
                    final JSONArray jarr2 = (JSONArray) JSONValue.parse(res.getString("maptasks"));
                    this.tasks[i[0]] = new byte[jarr.size()];
                    this.maptasks[i[0]] = new byte[this.tasks.length];
                    for (byte j = 0; j < jarr.size(); ++j) {
                        this.tasks[i[0]][j] = Byte.parseByte(jarr.get((int) j).toString());
                        this.maptasks[i[0]][j] = Byte.parseByte(jarr2.get((int) j).toString());
                    }
                    ++i[0];
                }
                res.close();

            });

            i[0] = 0;
            SQLManager.executeQuery("SELECT * FROM `level`;", (res) -> {

                while (res.next()) {
                    final Level level = new Level();
                    level.level = Integer.parseInt(res.getString("level"));
                    level.exps = Long.parseLong(res.getString("exps"));
                    level.ppoint = Short.parseShort(res.getString("ppoint"));
                    level.spoint = Short.parseShort(res.getString("spoint"));
                    Level.addLevel(level);
                    ++i[0];
                }
                res.close();

            });
            Level.onFinishAddLevel();
            i[0] = 0;
            SQLManager.executeQuery("SELECT * FROM `effect`;", (res) -> {

                while (res.next()) {
                    final EffectData eff = new EffectData();
                    eff.id = Byte.parseByte(res.getString("id"));
                    eff.type = Byte.parseByte(res.getString("type"));
                    eff.name = res.getString("name");
                    eff.iconId = Short.parseShort(res.getString("iconId"));
                    EffectData.entrys.add(eff);
                    ++i[0];
                }
                res.close();

            });

            i[0] = 0;
            
            SQLManager.executeQuery("SELECT * FROM `item`;", (res) -> {
                while (res.next()) {
                    final ItemData item = new ItemData();
                    item.id = Short.parseShort(res.getString("id"));
                    item.type = Byte.parseByte(res.getString("type"));
                    item.nclass = Byte.parseByte(res.getString("class"));
                    item.skill = Byte.parseByte(res.getString("skill"));
                    item.gender = Byte.parseByte(res.getString("gender"));
                    item.name = res.getString("name");
                    item.description = res.getString("description");
                    item.level = Byte.parseByte(res.getString("level"));
                    item.iconID = Short.parseShort(res.getString("iconID"));
                    item.part = Short.parseShort(res.getString("part"));
                    item.isUpToUp = (Byte.parseByte(res.getString("uptoup")) == 1);
                    item.isExpires = (Byte.parseByte(res.getString("isExpires")) == 1);
                    item.seconds_expires = Long.parseLong(res.getString("secondsExpires"));
                    item.saleCoinLock = Integer.parseInt(res.getString("saleCoinLock"));
                    item.itemoption = new ArrayList<Option>();
                    JSONArray Option = (JSONArray) JSONValue.parse(res.getString("ItemOption"));
                    for (int k = 0; k < Option.size(); ++k) {
                        
                        final JSONObject job = (JSONObject) Option.get(k);
                        final Option option = new Option(Integer.parseInt(job.get((Object) "id").toString()), Integer.parseInt(job.get((Object) "param").toString()));
                        item.itemoption.add(option);
                    }
                    item.option1 = new ArrayList<Option>();
                    Option = (JSONArray) JSONValue.parse(res.getString("Option1"));
                    for (int k = 0; k < Option.size(); ++k) {
                        final JSONObject job = (JSONObject) Option.get(k);
                        final Option option = new Option(Integer.parseInt(job.get((Object) "id").toString()), Integer.parseInt(job.get((Object) "param").toString()));
                        item.option1.add(option);
                    }
                    item.option2 = new ArrayList<Option>();
                    Option = (JSONArray) JSONValue.parse(res.getString("Option2"));
                    for (int k = 0; k < Option.size(); ++k) {
                        final JSONObject job = (JSONObject) Option.get(k);
                        final Option option = new Option(Integer.parseInt(job.get((Object) "id").toString()), Integer.parseInt(job.get((Object) "param").toString()));
                        item.option2.add(option);
                    }
                    item.option3 = new ArrayList<Option>();
                    Option = (JSONArray) JSONValue.parse(res.getString("Option3"));
                    for (int k = 0; k < Option.size(); ++k) {
                        final JSONObject job = (JSONObject) Option.get(k);
                        final Option option = new Option(Integer.parseInt(job.get((Object) "id").toString()), Integer.parseInt(job.get((Object) "param").toString()));
                        item.option3.add(option);
                    }
                    entrys.put((int) item.id, item);
                    ++i[0];
                }
                res.close();

            });

            i[0] = 0;
            SQLManager.executeQuery("SELECT * FROM `mob`;", (res) -> {

                while (res.next()) {
                    final MobData md = new MobData();
                    md.id = Integer.parseInt(res.getString("id"));
                    md.type = Byte.parseByte(res.getString("type"));
                    md.name = res.getString("name");
                    md.hp = Integer.parseInt(res.getString("hp"));
                    md.rangeMove = Byte.parseByte(res.getString("rangeMove"));
                    md.speed = Byte.parseByte(res.getString("speed"));

                    MobData.entrys.put(md.id, md);
                    ++i[0];
                }
                res.close();

            });

            i[0] = 0;
            SQLManager.executeQuery("SELECT * FROM `npc`;", (res) -> {
                if (res.last()) {
                    npcs = new NpcTemplate[res.getRow()];
                    res.beforeFirst();
                }
                while (res.next()) {
                    Npc npc = new Npc();
                    npc.id = res.getByte("id");
                    npc.name = res.getString("name");
                    npc.head = res.getShort("head");
                    npc.body = res.getShort("body");
                    npc.leg = res.getShort("leg");
                    npc.type = res.getByte("type");
                    Npc.npcTemplates.put(npc.id, npc);

                    final NpcTemplate npc2 = new NpcTemplate();
                    npc2.id = res.getInt("id");
                    npc2.name = res.getString("name");
                    npc2.headId = res.getShort("head");
                    npc2.bodyId = res.getShort("body");
                    npc2.legId = res.getShort("leg");
                    JSONArray jArr = (JSONArray) JSONValue.parse(res.getString("talk"));
                    int size = jArr.size();
                    npc2.menu = new String[size][];
                    for (int j = 0; j < size; j++) {
                        JSONArray jArr2 = (JSONArray) JSONValue.parse(jArr.get(j).toString());
                        int size2 = jArr2.size();
                        npc2.menu[j] = new String[size2];
                        for (int a = 0; a < size2; a++) {
                            npc2.menu[j][a] = jArr2.get(a).toString();
                        }
                    }
                    npcs[i[0]] = npc2;
                    ++i[0];
                }
            });

            i[0] = 0;
//            SQLManager.executeQuery("SELECT * FROM `map`;", (res) -> {
//
//                if (res.last()) {
//                    MapTemplate.arrTemplate = new MapTemplate[res.getRow()];
//                    res.beforeFirst();
//                }
//                while (res.next()) {
//                    final MapTemplate temp = new MapTemplate();
//                    temp.id = res.getInt("id");
//                    temp.tileID = res.getByte("tileId");
//                    temp.bgID = res.getByte("bgID");
//                    temp.name = res.getString("name");
//                    temp.typeMap = res.getByte("typeMap");
//                    temp.maxplayers = res.getByte("maxplayer");
//                    temp.numarea = res.getByte("numzone");
//                    temp.x0 = res.getShort("x0");
//                    temp.y0 = res.getShort("y0");
//                    JSONArray jarr3 = (JSONArray) JSONValue.parse(res.getString("Vgo"));
//                    temp.vgo = new Vgo[jarr3.size()];
//                    for (byte j = 0; j < jarr3.size(); ++j) {
//                        temp.vgo[j] = new Vgo();
//                        final JSONArray jar2 = (JSONArray) JSONValue.parse(jarr3.get((int) j).toString());
//                        final Vgo vg = temp.vgo[j];
//                        vg.minX = Short.parseShort(jar2.get(0).toString());
//                        vg.minY = Short.parseShort(jar2.get(1).toString());
//                        vg.maxX = Short.parseShort(jar2.get(2).toString());
//                        if (vg.maxX == -1) {
//                            vg.maxX = (short) (vg.minX + 24);
//                        }
//                        vg.maxY = Short.parseShort(jar2.get(3).toString());
//                        if (vg.maxY == -1) {
//                            vg.maxY = (short) (vg.minY + 24);
//                        }
//                        vg.mapid = Short.parseShort(jar2.get(4).toString());
//                        vg.goX = Short.parseShort(jar2.get(5).toString());
//                        vg.goY = Short.parseShort(jar2.get(6).toString());
//                    }
//                    jarr3 = (JSONArray) JSONValue.parse(res.getString("Mob"));
//                    temp.arMobid = new short[jarr3.size()];
//                    temp.arrMobx = new short[jarr3.size()];
//                    temp.arrMoby = new short[jarr3.size()];
//                    temp.arrMobstatus = new byte[jarr3.size()];
//                    temp.arrMoblevel = new int[jarr3.size()];
//                    temp.arrLevelboss = new byte[jarr3.size()];
//                    temp.arrisboss = new boolean[jarr3.size()];
//                    for (short l = 0; l < jarr3.size(); ++l) {
//                        final JSONArray entry = (JSONArray) jarr3.get((int) l);
//                        temp.arMobid[l] = Short.parseShort(entry.get(0).toString());
//                        temp.arrMoblevel[l] = Integer.parseInt(entry.get(1).toString());
//                        temp.arrMobx[l] = Short.parseShort(entry.get(2).toString());
//                        temp.arrMoby[l] = Short.parseShort(entry.get(3).toString());
//                        temp.arrMobstatus[l] = Byte.parseByte(entry.get(4).toString());
//                        temp.arrLevelboss[l] = Byte.parseByte(entry.get(5).toString());
//                        temp.arrisboss[l] = Boolean.parseBoolean(entry.get(6).toString());
//                    }
//                    jarr3 = (JSONArray) JSONValue.parse(res.getString("NPC"));
//                    temp.npc = new Npc[jarr3.size()];
//                    for (byte j = 0; j < jarr3.size(); ++j) {
//                        temp.npc[j] = new Npc();
//                        final JSONArray jar2 = (JSONArray) JSONValue.parse(jarr3.get((int) j).toString());
//                        final Npc npc = temp.npc[j];
//                        npc.type = Byte.parseByte(jar2.get(0).toString());
//                        npc.x = Short.parseShort(jar2.get(1).toString());
//                        npc.y = Short.parseShort(jar2.get(2).toString());
//                        npc.id = Byte.parseByte(jar2.get(3).toString());
//                    }
//                    jarr3 = (JSONArray) JSONValue.parse(res.getString("itemtree"));
//                    temp.itemtree = new ItemTree[jarr3.size()];
//                    for (byte j = 0; j < jarr3.size(); j++) {
//                        temp.itemtree[j] = new ItemTree();
//                        JSONArray jar2 = (JSONArray) JSONValue.parse(jarr3.get(j).toString());
//                        ItemTree itr = temp.itemtree[j];
//                        itr.num3 = Short.parseShort(jar2.get(0).toString());
//                        itr.k2 = Short.parseShort(jar2.get(1).toString());
//                    }
//                    MapTemplate.arrTemplate[i[0]] = temp;
//                    ++i[0];
//                }
//                res.close();
//
//            });
SQLManager.executeQuery("SELECT * FROM `map`;", (res) -> {

                if (res.last()) {
                    MapTemplate.arrTemplate = new MapTemplate[res.getRow()];
                    res.beforeFirst();
                }
                while (res.next()) {
                    final MapTemplate temp = new MapTemplate();
                    temp.id = res.getInt("id");
                    temp.tileID = res.getByte("tileId");
                    temp.bgID = res.getByte("bgID");
                    temp.name = res.getString("name");
                    temp.typeMap = res.getByte("typeMap");
                    temp.maxplayers = res.getByte("maxplayer");
                    temp.numarea = res.getByte("numzone");
                    temp.x0 = res.getShort("x0");
                    temp.y0 = res.getShort("y0");
                    if(temp.id!=22){
                        ByteArrayInputStream bais = new ByteArrayInputStream(GameScr.loadFile("res/map/"+temp.id).toByteArray());
                    DataInputStream dis = new DataInputStream(bais);
                    temp.max_x = dis.readByte()*24;
                    temp.max_y = dis.readByte()*24;
                    dis.close();
                    bais.close();
                    }else{
                        ByteArrayInputStream bais = new ByteArrayInputStream(GameScr.loadFile("res/map/"+temp.id).toByteArray());
                        DataInputStream dis = new DataInputStream(bais);
                        temp.max_x = dis.readByte()*240;
                        temp.max_y = dis.readByte()*240;
                        dis.close();
                        bais.close();
                    }
                    
                    JSONArray jarr3 = (JSONArray) JSONValue.parse(res.getString("Vgo"));
                    temp.vgo = new Vgo[jarr3.size()];
                    for (byte j = 0; j < jarr3.size(); ++j) {
                        temp.vgo[j] = new Vgo();
                        final JSONArray jar2 = (JSONArray) JSONValue.parse(jarr3.get((int) j).toString());
                        final Vgo vg = temp.vgo[j];
                        vg.minX = Short.parseShort(jar2.get(0).toString());
                        vg.minY = Short.parseShort(jar2.get(1).toString());
                        vg.maxX = Short.parseShort(jar2.get(2).toString());
                        if (vg.maxX == -1) {
                            vg.maxX = (short) (vg.minX + 24);
                        }
                        vg.maxY = Short.parseShort(jar2.get(3).toString());
                        if (vg.maxY == -1) {
                            vg.maxY = (short) (vg.minY + 24);
                        }
                        vg.mapid = Short.parseShort(jar2.get(4).toString());
                        vg.goX = Short.parseShort(jar2.get(5).toString());
                        vg.goY = Short.parseShort(jar2.get(6).toString());
                    }
                    jarr3 = (JSONArray) JSONValue.parse(res.getString("Mob"));
                    temp.arMobid = new short[jarr3.size()];
                    temp.arrMobx = new short[jarr3.size()];
                    temp.arrMoby = new short[jarr3.size()];
                    temp.arrMobstatus = new byte[jarr3.size()];
                    temp.arrMoblevel = new int[jarr3.size()];
                    temp.arrLevelboss = new byte[jarr3.size()];
                    temp.arrisboss = new boolean[jarr3.size()];
                    for (short l = 0; l < jarr3.size(); ++l) {
                        final JSONArray entry = (JSONArray) jarr3.get((int) l);
                        temp.arMobid[l] = Short.parseShort(entry.get(0).toString());
                        temp.arrMoblevel[l] = Integer.parseInt(entry.get(1).toString());
                        temp.arrMobx[l] = Short.parseShort(entry.get(2).toString());
                        temp.arrMoby[l] = Short.parseShort(entry.get(3).toString());
                        temp.arrMobstatus[l] = Byte.parseByte(entry.get(4).toString());
                        temp.arrLevelboss[l] = Byte.parseByte(entry.get(5).toString());
                        temp.arrisboss[l] = Boolean.parseBoolean(entry.get(6).toString());
                    }
                    jarr3 = (JSONArray) JSONValue.parse(res.getString("NPC"));
                    temp.npc = new Npc[jarr3.size()];
                    for (byte j = 0; j < jarr3.size(); ++j) {
                        temp.npc[j] = new Npc();
                        final JSONArray jar2 = (JSONArray) JSONValue.parse(jarr3.get((int) j).toString());
                        final Npc npc = temp.npc[j];
                        npc.type = Byte.parseByte(jar2.get(0).toString());
                        npc.x = Short.parseShort(jar2.get(1).toString());
                        npc.y = Short.parseShort(jar2.get(2).toString());
                        npc.id = Byte.parseByte(jar2.get(3).toString());
                    }
                    jarr3 = (JSONArray) JSONValue.parse(res.getString("itemtree"));
                    temp.itemtree = new ItemTree[jarr3.size()];
                    for (byte j = 0; j < jarr3.size(); j++) {
                        temp.itemtree[j] = new ItemTree();
                        JSONArray jar2 = (JSONArray) JSONValue.parse(jarr3.get(j).toString());
                        ItemTree itr = temp.itemtree[j];
                        itr.num3 = Short.parseShort(jar2.get(0).toString());
                        itr.k2 = Short.parseShort(jar2.get(1).toString());
                    }
                    MapTemplate.arrTemplate[i[0]] = temp;
                    ++i[0];
                }
                res.close();

            });
            i[0] = 0;
            SQLManager.executeQuery("SELECT * FROM `skill`;", (res) -> {

                while (res.next()) {
                    final SkillData skill = new SkillData();
                    skill.id = Short.parseShort(res.getString("id"));
                    skill.nclass = Byte.parseByte(res.getString("class"));
                    skill.name = res.getString("name");
                    skill.maxPoint = Byte.parseByte(res.getString("maxPoint"));
                    skill.type = Byte.parseByte(res.getString("type"));
                    skill.iconId = Short.parseShort(res.getString("iconId"));
                    skill.desc = res.getString("desc");
                    final JSONArray Skilltemplate = (JSONArray) JSONValue.parse(res.getString("SkillTemplates"));
                    for (final Object template : Skilltemplate) {
                        final JSONObject job2 = (JSONObject) template;
                        final SkillTemplates temp2 = new SkillTemplates();
                        temp2.skillId = Short.parseShort(job2.get((Object) "skillId").toString());
                        temp2.point = Byte.parseByte(job2.get((Object) "point").toString());
                        temp2.level = Integer.parseInt(job2.get((Object) "level").toString());
                        temp2.manaUse = Short.parseShort(job2.get((Object) "manaUse").toString());
                        temp2.coolDown = Integer.parseInt(job2.get((Object) "coolDown").toString());
                        temp2.dx = Short.parseShort(job2.get((Object) "dx").toString());
                        temp2.dy = Short.parseShort(job2.get((Object) "dy").toString());
                        temp2.maxFight = Byte.parseByte(job2.get((Object) "maxFight").toString());
                        final JSONArray Option2 = (JSONArray) JSONValue.parse(job2.get((Object) "options").toString());
                        for (final Object option2 : Option2) {
                            final JSONObject job3 = (JSONObject) option2;
                            final Option op = new Option(Integer.parseInt(job3.get((Object) "id").toString()), Integer.parseInt(job3.get((Object) "param").toString()));
                            temp2.options.add(op);
                        }
                        skill.templates.add(temp2);
                    }
                    SkillData.entrys.put(skill.id, skill);
                    ++i[0];
                }
                res.close();

            });

            i[0] = 0;
            SQLManager.executeQuery("SELECT * FROM `itemsell`;", (res) -> {

                while (res.next()) {
                    final ItemSell sell = new ItemSell();
                    sell.id = Integer.parseInt(res.getString("id"));
                    sell.type = Byte.parseByte(res.getString("type"));
                    final JSONArray jar3 = (JSONArray) JSONValue.parse(res.getString("ListItem"));
                    if (jar3 != null) {
                        sell.item = new Item[jar3.size()];
                        for (byte j = 0; j < jar3.size(); ++j) {
                            final JSONObject job = (JSONObject) jar3.get((int) j);
                            final Item item2 = parseItem(jar3.get((int) j).toString());
                            item2.buyCoin = Integer.parseInt(job.get((Object) "buyCoin").toString());
                            item2.buyCoinLock = Integer.parseInt(job.get((Object) "buyCoinLock").toString());
                            item2.buyGold = Integer.parseInt(job.get((Object) "buyGold").toString());
                            sell.item[j] = item2;
                        }
                    }
                    ItemSell.entrys.put((int) sell.type, sell);
                    ++i[0];
                }
                res.close();

            });

            SQLManager.executeQuery("SELECT * from npc_daily limit 1", (red) -> {
                if (red.first()) {
                    MENU_EVENT_NPC = Mapper.converter.readValue(red.getString("features"), String[].class);
                    ID_EVENT_NPC = Short.parseShort(red.getInt("id") + "");
                    EVENT_NPC_CHAT = Mapper.converter.readValue(red.getString("npc_chat"), String[].class);
                    ID_FEATURES = Mapper.converter.readValue(red.getString("features_id"), short[].class);
                } else {
                    System.out.println("Khong tim thay NPC MENU");
                }
                red.close();
            });

        } catch (Exception e) {
            util.Debug("Error i:" + i[0]);
            e.printStackTrace();
            System.exit(0);
        }
        this.loadGame();

    }
private void loadVersion() {
        System.out.println("batdau loadversion");
        try {
            try (ByteArrayOutputStream bas = new ByteArrayOutputStream()) {
                DataOutputStream dos = new DataOutputStream(bas);
                dos.writeByte(this.vsData);
                dos.writeByte(this.vsMap);
                dos.writeByte(this.vsSkill);
                dos.writeByte(this.vsItem);
                byte[] abc = GameScr.loadFile("res/cache/cacheVersion").toByteArray();
                int i;
                for (i = 0; i < abc.length; i++) {
                    dos.writeByte(abc[i]);
                }
                byte[] ab = bas.toByteArray();
                GameScr.saveFile("res/cache/version", ab);
                dos.close();
            }
        } catch (Exception var4) {
            var4.printStackTrace();
        }

    }

    private void loadGame() {
        System.out.println("bat dau load game");
        int i = 0;
        try {
            i = 0;
            SQLManager.executeQuery("SELECT * FROM `clan`;", (res) -> {

                while (res.next()) {
                    final ClanManager clan = new ClanManager();
                    clan.id = Integer.parseInt(res.getString("id"));
                    clan.name = res.getString("name");
                    clan.exp = res.getInt("exp");
                    clan.setLevel(res.getInt("level"));
                    clan.itemLevel = res.getInt("itemLevel");
                    clan.coin = res.getInt("coin");
                    clan.reg_date = res.getString("reg_date");
                    clan.log = res.getString("log");
                    clan.alert = res.getString("alert");
                    clan.use_card = res.getByte("use_card");
                    clan.openDun = res.getByte("openDun");
                    clan.debt = res.getByte("debt");
                    JSONArray jar = (JSONArray) JSONValue.parse(res.getString("members"));
                    if (jar != null) {
                        for (short j = 0; j < jar.size(); ++j) {
                            final JSONArray jar2 = (JSONArray) jar.get((int) j);
                            val mem = ClanMember.fromJSONArray(jar2);
                            clan.members.add(mem);
                        }
                    }

                    try {
                        clan.clanThanThus = Mapper.converter.readValue(res.getString("clan_than_thu"), new TypeReference<List<ClanThanThu>>() {
                        });
                    } catch (Exception e) {
                        clan.clanThanThus = new ArrayList<>();
                    }
                    jar = (JSONArray) JSONValue.parse(res.getString("items"));
                    if (jar != null) {
                        for (byte k = 0; k < jar.size(); ++k) {
                            clan.items.add(parseItem(jar.get((int) k).toString()));
                        }
                    }
                    clan.week = res.getString("week");
                    try {
                        clan.setClanBattleData(Mapper.converter.readValue(res.getString("clan_battle_data"), ClanBattleData.class));
                    } catch (Exception e) {
                    }
                    ClanManager.entrys.add(clan);
                }
                res.close();

            });
            ItemShinwaManager.loadFromDatabase();

            SQLManager.executeUpdate("UPDATE `ninja` SET `caveID`=-1;");
        } catch (Exception e) {
            util.Debug("Error i:" + i);
            e.printStackTrace();
            System.exit(0);
        }
    }

//    public void getPackMessage(final User p) throws IOException {
//        final Message m = new Message(-28);
//        m.writer().writeByte(-123);
//        m.writer().writeByte(this.vsData);
//        m.writer().writeByte(this.vsMap);
//        m.writer().writeByte(this.vsSkill);
//        m.writer().writeByte(this.vsItem);
//        m.writer().writeByte(0);
//        m.writer().writeByte(0);
//        m.writer().writeByte(0);
//        m.writer().flush();
//        p.sendMessage(m);
//        m.cleanup();
//    }
    public void getPackMessage(final User p) throws IOException {
        Message m = null;
        try {
            m = new Message(-28);
            m.writer().writeByte(-123);
            byte[] ab = GameScr.loadFile("res/cache/version").toByteArray();
            m.writer().write(ab);
            m.writer().flush();
            p.session.sendMessage(m);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (m != null) {
                m.cleanup();
            }
        }
    }

    public void sendData(final User p) throws IOException {
        final Message m = new Message(-28);
        m.writer().writeByte(-122);
        m.writer().writeByte(this.vsData);
        byte[] ab = GameScr.loadFile("res/cache/data/nj_arrow").toByteArray();
        m.writer().writeInt(ab.length);
        m.writer().write(ab);
        ab = GameScr.loadFile("res/cache/data/nj_effect").toByteArray();
        m.writer().writeInt(ab.length);
        m.writer().write(ab);
        ab = GameScr.loadFile("res/cache/data/nj_image").toByteArray();
        m.writer().writeInt(ab.length);
        m.writer().write(ab);
        ab = GameScr.loadFile("res/cache/data/nj_part").toByteArray();
        m.writer().writeInt(ab.length);
        m.writer().write(ab);
        ab = GameScr.loadFile("res/cache/data/nj_skill").toByteArray();
        m.writer().writeInt(ab.length);
        m.writer().write(ab);
        m.writer().writeByte(this.tasks.length);
        for (byte i = 0; i < this.tasks.length; ++i) {
            m.writer().writeByte(this.tasks[i].length);
            for (byte j = 0; j < this.tasks[i].length; ++j) {
                m.writer().writeByte(this.tasks[i][j]);
                m.writer().writeByte(this.maptasks[i][j]);
            }
        }
        m.writer().writeByte(Level.getEntrys().size());
        for (final Level entry : Level.getEntrys()) {
            m.writer().writeLong(entry.exps);
        }
        m.writer().writeByte(GameScr.crystals.length);
        for (byte i = 0; i < GameScr.crystals.length; ++i) {
            m.writer().writeInt(GameScr.crystals[i]);
        }
        m.writer().writeByte(GameScr.upClothe.length);
        for (byte i = 0; i < GameScr.upClothe.length; ++i) {
            m.writer().writeInt(GameScr.upClothe[i]);
        }
        m.writer().writeByte(GameScr.upAdorn.length);
        for (byte i = 0; i < GameScr.upAdorn.length; ++i) {
            m.writer().writeInt(GameScr.upAdorn[i]);
        }
        m.writer().writeByte(GameScr.upWeapon.length);
        for (byte i = 0; i < GameScr.upWeapon.length; ++i) {
            m.writer().writeInt(GameScr.upWeapon[i]);
        }
        m.writer().writeByte(GameScr.coinUpCrystals.length);
        for (byte i = 0; i < GameScr.coinUpCrystals.length; ++i) {
            m.writer().writeInt(GameScr.coinUpCrystals[i]);
        }
        m.writer().writeByte(GameScr.coinUpClothes.length);
        for (byte i = 0; i < GameScr.coinUpClothes.length; ++i) {
            m.writer().writeInt(GameScr.coinUpClothes[i]);
        }
        m.writer().writeByte(GameScr.coinUpAdorns.length);
        for (byte i = 0; i < GameScr.coinUpAdorns.length; ++i) {
            m.writer().writeInt(GameScr.coinUpAdorns[i]);
        }
        m.writer().writeByte(GameScr.coinUpWeapons.length);
        for (byte i = 0; i < GameScr.coinUpWeapons.length; ++i) {
            m.writer().writeInt(GameScr.coinUpWeapons[i]);
        }
        m.writer().writeByte(GameScr.goldUps.length);
        for (byte i = 0; i < GameScr.goldUps.length; ++i) {
            m.writer().writeInt(GameScr.goldUps[i]);
        }
        m.writer().writeByte(GameScr.maxPercents.length);
        for (byte i = 0; i < GameScr.maxPercents.length; ++i) {
            m.writer().writeInt(GameScr.maxPercents[i]);
        }
        m.writer().writeByte(EffectData.entrys.size());
        for (byte i = 0; i < EffectData.entrys.size(); ++i) {
            m.writer().writeByte(EffectData.entrys.get(i).id);
            m.writer().writeByte(EffectData.entrys.get(i).type);
            m.writer().writeUTF(EffectData.entrys.get(i).name);
            m.writer().writeShort(EffectData.entrys.get(i).iconId);
        }
        m.writer().flush();
        p.sendMessage(m);
        m.cleanup();
    }

    public static void chatKTG(final String chat) throws IOException {
        final Message m = new Message(-25);
        m.writer().writeUTF(chat);
        m.writer().flush();
        PlayerManager.getInstance().NinjaMessage(m);
        m.cleanup();
    }

    public void Infochat(final String chat) throws IOException {
        final Message m = new Message(-24);
        m.writer().writeUTF(chat);
        m.writer().flush();
        PlayerManager.getInstance().NinjaMessage(m);
        m.cleanup();
    }

    protected void stop() {
    }

    public void chatKTG(final User p, final Message m) throws IOException {
        final String chat = m.reader().readUTF();
        m.cleanup();
        if (p.chatKTGdelay > System.currentTimeMillis()) {
            p.session.sendMessageLog("Chờ sau " + (p.chatKTGdelay - System.currentTimeMillis()) / 1000L + "s.");
            return;
        }
        p.chatKTGdelay = System.currentTimeMillis() + 5000L;
        if (p.luong < 50) {
            p.session.sendMessageLog("Bạn không có đủ lượng trên người.");
            return;
        }
        p.luongMessage(-5L);
        serverChat(p.nj.name, chat);
    }

    public static void serverChat(final String name, final String s) {
        final Message m = new Message(-21);
        try {
            m.writer().writeUTF(name);
            m.writer().writeUTF(s);
            m.writer().flush();
            PlayerManager.getInstance().NinjaMessage(m);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (m != null) {
                m.cleanup();
            }
        }
    }

    public void sendTB(final User p, final String title, final String s) throws IOException {
        final Message m = new Message(53);
        m.writer().writeUTF(title);
        m.writer().writeUTF(s);
        m.writer().flush();
        p.sendMessage(m);
        m.cleanup();
    }

    public void close() {
        for (int i = 0; i < this.rotationluck.length; ++i) {
            this.rotationluck[i].close();
            this.rotationluck[i] = null;
        }
        this.rotationluck = null;
        for (int i = 0; i < Manager.server.getMaps().length; ++i) {
            val map = Manager.server.getMaps()[i];
            if (map != null) {
                Manager.server.getMaps()[i].close();
                Manager.server.getMaps()[i] = null;
            }
        }
        Manager.server.setMaps(null);
    }

    public static ByteArrayOutputStream[] cache = new ByteArrayOutputStream[5];

//    static {
//        
//    }
    private void loadFileToJar() {
        Manager.server = Server.getInstance();
        cache[0] = GameScr.loadFile("res/cache/data.bin");
        cache[1] = GameScr.loadFile("res/cache/map");
        cache[2] = GameScr.loadFile("res/cache/skill");
        cache[3] = GameScr.loadFile("res/cache/item");
        cache[4] = GameScr.loadFile("res/cache/skillnhanban");
    }

    public void sendMap(final User p) throws IOException {
//        final Message m = new Message(-28);
//        m.writer().writeByte(-121);
//        m.writer().write(cache[1].toByteArray());
//        m.writer().flush();
//        p.sendMessage(m);
//        m.cleanup();
        Message m = new Message(-28);
        try {
            m.writer().writeByte(-121);
            m.writer().writeByte(this.vsMap);
            m.writer().writeByte(MapTemplate.arrTemplate.length);
            for (short i = 0; i < MapTemplate.arrTemplate.length; ++i) {
                m.writer().writeUTF(MapTemplate.arrTemplate[i].name);
            }
            m.writer().writeByte(npcs.length);
            for (short i = 0; i < npcs.length; ++i) {
                m.writer().writeUTF(npcs[i].name);
                m.writer().writeShort(npcs[i].headId);
                m.writer().writeShort(npcs[i].bodyId);
                m.writer().writeShort(npcs[i].legId);
                String[][] menu = npcs[i].menu;
                m.writer().writeByte(menu.length);
                for (String[] me : menu) {
                    m.writer().writeByte(me.length);
                    for (String s : me) {
                        m.writer().writeUTF(s);
                    }
                }
            }
            m.writer().writeByte(MobData.entrys.size());
            for (int i = 0; i < MobData.entrys.size(); i++) {
                m.writer().writeByte(MobData.entrys.get(i).type);
                m.writer().writeUTF(MobData.entrys.get(i).name);
                m.writer().writeInt(MobData.entrys.get(i).hp);
                m.writer().writeByte(MobData.entrys.get(i).rangeMove);
                m.writer().writeByte(MobData.entrys.get(i).speed);
            }
            m.writer().flush();
            p.sendMessage(m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (m != null) {
                m.cleanup();
            }
        }
    }

    public void sendSkil1l(final User p) throws IOException {
        final Message m = new Message(-28);
        m.writer().writeByte(-120);
        // m.writer().write(cache[2].toByteArray());
        m.writer().write(GameScr.loadFile("res/cache/skill").toByteArray());
        m.writer().flush();
        p.session.sendMessage(m);
        m.cleanup();

    }

    public void sendSkill(User p) throws IOException {
        Message m = new Message(-28);
        try {
            m.writer().writeByte(-120);
            m.writer().writeByte(this.vsSkill);
//            System.out.println("----------------------------------------------this.vskill"+this.vsSkill);
            m.writer().writeByte(Manager.sOptionTemplates.length);
            for (short i = 0; i < Manager.sOptionTemplates.length; ++i) {
                m.writer().writeUTF(Manager.sOptionTemplates[i].name);
            }
            m.writer().writeByte(7);//nclasssize
            m.writer().writeUTF("Ninja Vô Danh");
            m.writer().writeByte(1);//skillsize
            ArrayList<SkillData> entrys1 = new ArrayList<SkillData>(SkillData.entrys.values());
            for (short j = 0; j < entrys1.size(); ++j) {
                if (entrys1.get(j).nclass == 0) {
                    m.writer().writeByte(entrys1.get(j).id);
                    m.writer().writeUTF(entrys1.get(j).name);
                    m.writer().writeByte(entrys1.get(j).maxPoint);
                    m.writer().writeByte(entrys1.get(j).type);
                    m.writer().writeShort(entrys1.get(j).iconId);
                    m.writer().writeUTF(entrys1.get(j).desc);
                    m.writer().writeByte(entrys1.get(j).templates.size());
                    //System.out.println("SkillData.entrys.get(j).templates.size()" + entrys1.get(j).templates.size());
                    for (int k = 0; k < entrys1.get(j).templates.size(); k++) {
                        //System.out.println("SkillData.entrys.get(j).templates.get(k)" + SkillData.entrys.get(j).templates.toString());
                        m.writer().writeShort(entrys1.get(j).templates.get(k).skillId);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).point);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).level);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).manaUse);
                        m.writer().writeInt(entrys1.get(j).templates.get(k).coolDown);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).dx);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).dy);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).maxFight);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).options.size());
                        for (int l = 0; l < entrys1.get(j).templates.get(k).options.size(); l++) {
                            //System.out.println("SkillData.entrys.get(j).templates.get(k).options.size()" + SkillData.entrys.get(j).templates.get(k).options.toString());
                            m.writer().writeShort(entrys1.get(j).templates.get(k).options.get(l).param);
                            m.writer().writeByte(entrys1.get(j).templates.get(k).options.get(l).id);
                        }
                    }
                }
            }
            m.writer().writeUTF("Ninja Kiếm");
            m.writer().writeByte(15);
            for (short j = 0; j < entrys1.size(); ++j) {
                if (entrys1.get(j).nclass == 1) {
                    m.writer().writeByte(entrys1.get(j).id);
                    m.writer().writeUTF(entrys1.get(j).name);
                    m.writer().writeByte(entrys1.get(j).maxPoint);
                    m.writer().writeByte(entrys1.get(j).type);
                    m.writer().writeShort(entrys1.get(j).iconId);
                    m.writer().writeUTF(entrys1.get(j).desc);
                    m.writer().writeByte(entrys1.get(j).templates.size());
                    //System.out.println("SkillData.entrys.get(j).templates.size()" + entrys1.get(j).templates.size());
                    for (int k = 0; k < entrys1.get(j).templates.size(); k++) {
                        //System.out.println("SkillData.entrys.get(j).templates.get(k)" + SkillData.entrys.get(j).templates.toString());
                        m.writer().writeShort(entrys1.get(j).templates.get(k).skillId);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).point);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).level);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).manaUse);
                        m.writer().writeInt(entrys1.get(j).templates.get(k).coolDown);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).dx);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).dy);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).maxFight);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).options.size());
                        for (int l = 0; l < entrys1.get(j).templates.get(k).options.size(); l++) {
                            //System.out.println("SkillData.entrys.get(j).templates.get(k).options.size()" + SkillData.entrys.get(j).templates.get(k).options.toString());
                            m.writer().writeShort(entrys1.get(j).templates.get(k).options.get(l).param);
                            m.writer().writeByte(entrys1.get(j).templates.get(k).options.get(l).id);
                        }
                    }
                }
            }
            m.writer().writeUTF("Ninja Phi Tiêu");
            m.writer().writeByte(15);
            for (short j = 0; j < entrys1.size(); ++j) {
                if (entrys1.get(j).nclass == 2) {
                    m.writer().writeByte(entrys1.get(j).id);
                    m.writer().writeUTF(entrys1.get(j).name);
                    m.writer().writeByte(entrys1.get(j).maxPoint);
                    m.writer().writeByte(entrys1.get(j).type);
                    m.writer().writeShort(entrys1.get(j).iconId);
                    m.writer().writeUTF(entrys1.get(j).desc);
                    m.writer().writeByte(entrys1.get(j).templates.size());
                    //System.out.println("SkillData.entrys.get(j).templates.size()" + SkillData.entrys.get(j).templates.size());
                    for (int k = 0; k < entrys1.get(j).templates.size(); k++) {
                        //System.out.println("SkillData.entrys.get(j).templates.get(k)" + SkillData.entrys.get(j).templates.toString());
                        m.writer().writeShort(entrys1.get(j).templates.get(k).skillId);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).point);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).level);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).manaUse);
                        m.writer().writeInt(entrys1.get(j).templates.get(k).coolDown);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).dx);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).dy);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).maxFight);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).options.size());
                        for (int l = 0; l < entrys1.get(j).templates.get(k).options.size(); l++) {
                            //System.out.println("SkillData.entrys.get(j).templates.get(k).options.size()" + SkillData.entrys.get(j).templates.get(k).options.toString());
                            m.writer().writeShort(entrys1.get(j).templates.get(k).options.get(l).param);
                            m.writer().writeByte(entrys1.get(j).templates.get(k).options.get(l).id);
                        }
                    }
                }
            }
            m.writer().writeUTF("Ninja Kunai");
            m.writer().writeByte(15);
            for (short j = 0; j < entrys1.size(); ++j) {
                if (entrys1.get(j).nclass == 3) {
                    m.writer().writeByte(entrys1.get(j).id);
                    m.writer().writeUTF(entrys1.get(j).name);
                    m.writer().writeByte(entrys1.get(j).maxPoint);
                    m.writer().writeByte(entrys1.get(j).type);
                    m.writer().writeShort(entrys1.get(j).iconId);
                    m.writer().writeUTF(entrys1.get(j).desc);
                    m.writer().writeByte(entrys1.get(j).templates.size());
                    //System.out.println("SkillData.entrys.get(j).templates.size()" + SkillData.entrys.get(j).templates.size());
                    for (int k = 0; k < entrys1.get(j).templates.size(); k++) {
                        //System.out.println("SkillData.entrys.get(j).templates.get(k)" + SkillData.entrys.get(j).templates.toString());
                        m.writer().writeShort(entrys1.get(j).templates.get(k).skillId);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).point);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).level);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).manaUse);
                        m.writer().writeInt(entrys1.get(j).templates.get(k).coolDown);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).dx);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).dy);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).maxFight);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).options.size());
                        for (int l = 0; l < entrys1.get(j).templates.get(k).options.size(); l++) {
                            //System.out.println("SkillData.entrys.get(j).templates.get(k).options.size()" + SkillData.entrys.get(j).templates.get(k).options.toString());
                            m.writer().writeShort(entrys1.get(j).templates.get(k).options.get(l).param);
                            m.writer().writeByte(entrys1.get(j).templates.get(k).options.get(l).id);
                        }
                    }
                }
            }
            m.writer().writeUTF("Ninja Cung");
            m.writer().writeByte(15);
            for (short j = 0; j < entrys1.size(); ++j) {
                if (entrys1.get(j).nclass == 4) {
                    m.writer().writeByte(entrys1.get(j).id);
                    m.writer().writeUTF(entrys1.get(j).name);
                    m.writer().writeByte(entrys1.get(j).maxPoint);
                    m.writer().writeByte(entrys1.get(j).type);
                    m.writer().writeShort(entrys1.get(j).iconId);
                    m.writer().writeUTF(entrys1.get(j).desc);
                    m.writer().writeByte(entrys1.get(j).templates.size());
                    //System.out.println("SkillData.entrys.get(j).templates.size()" + SkillData.entrys.get(j).templates.size());
                    for (int k = 0; k < entrys1.get(j).templates.size(); k++) {
                        //System.out.println("SkillData.entrys.get(j).templates.get(k)" + SkillData.entrys.get(j).templates.toString());
                        m.writer().writeShort(entrys1.get(j).templates.get(k).skillId);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).point);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).level);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).manaUse);
                        m.writer().writeInt(entrys1.get(j).templates.get(k).coolDown);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).dx);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).dy);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).maxFight);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).options.size());
                        for (int l = 0; l < entrys1.get(j).templates.get(k).options.size(); l++) {
                            //System.out.println("SkillData.entrys.get(j).templates.get(k).options.size()" + SkillData.entrys.get(j).templates.get(k).options.toString());
                            m.writer().writeShort(entrys1.get(j).templates.get(k).options.get(l).param);
                            m.writer().writeByte(entrys1.get(j).templates.get(k).options.get(l).id);
                        }
                    }
                }
            }
            m.writer().writeUTF("Ninja Đao");
            m.writer().writeByte(15);
            for (short j = 0; j < entrys1.size(); ++j) {
                if (entrys1.get(j).nclass == 5) {
                    m.writer().writeByte(entrys1.get(j).id);
                    m.writer().writeUTF(entrys1.get(j).name);
                    m.writer().writeByte(entrys1.get(j).maxPoint);
                    m.writer().writeByte(entrys1.get(j).type);
                    m.writer().writeShort(entrys1.get(j).iconId);
                    m.writer().writeUTF(entrys1.get(j).desc);
                    m.writer().writeByte(entrys1.get(j).templates.size());
                    //System.out.println("SkillData.entrys.get(j).templates.size()" + SkillData.entrys.get(j).templates.size());
                    for (int k = 0; k < entrys1.get(j).templates.size(); k++) {
                        //System.out.println("SkillData.entrys.get(j).templates.get(k)" + SkillData.entrys.get(j).templates.toString());
                        m.writer().writeShort(entrys1.get(j).templates.get(k).skillId);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).point);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).level);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).manaUse);
                        m.writer().writeInt(entrys1.get(j).templates.get(k).coolDown);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).dx);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).dy);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).maxFight);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).options.size());
                        for (int l = 0; l < entrys1.get(j).templates.get(k).options.size(); l++) {
                            //System.out.println("SkillData.entrys.get(j).templates.get(k).options.size()" + SkillData.entrys.get(j).templates.get(k).options.toString());
                            m.writer().writeShort(entrys1.get(j).templates.get(k).options.get(l).param);
                            m.writer().writeByte(entrys1.get(j).templates.get(k).options.get(l).id);
                        }
                    }
                }
            }
            m.writer().writeUTF("Ninja Quạt");
            m.writer().writeByte(15);
            for (short j = 0; j < entrys1.size(); ++j) {
                if (entrys1.get(j).nclass == 6) {
                    m.writer().writeByte(entrys1.get(j).id);
                    m.writer().writeUTF(entrys1.get(j).name);
                    m.writer().writeByte(entrys1.get(j).maxPoint);
                    m.writer().writeByte(entrys1.get(j).type);
                    m.writer().writeShort(entrys1.get(j).iconId);
                    m.writer().writeUTF(entrys1.get(j).desc);
                    m.writer().writeByte(entrys1.get(j).templates.size());
                    //System.out.println("SkillData.entrys.get(j).templates.size()" + SkillData.entrys.get(j).templates.size());
                    for (int k = 0; k < entrys1.get(j).templates.size(); k++) {
                        //System.out.println("SkillData.entrys.get(j).templates.get(k)" + SkillData.entrys.get(j).templates.toString());
                        m.writer().writeShort(entrys1.get(j).templates.get(k).skillId);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).point);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).level);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).manaUse);
                        m.writer().writeInt(entrys1.get(j).templates.get(k).coolDown);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).dx);
                        m.writer().writeShort(entrys1.get(j).templates.get(k).dy);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).maxFight);
                        m.writer().writeByte(entrys1.get(j).templates.get(k).options.size());
                        for (int l = 0; l < entrys1.get(j).templates.get(k).options.size(); l++) {
                            //System.out.println("SkillData.entrys.get(j).templates.get(k).options.size()" + SkillData.entrys.get(j).templates.get(k).options.toString());
                            m.writer().writeShort(entrys1.get(j).templates.get(k).options.get(l).param);
                            m.writer().writeByte(entrys1.get(j).templates.get(k).options.get(l).id);
                        }
                    }
                }
            }
//            m.writer().writeUTF("Ninja Hắc Ám");
//            m.writer().writeByte(9);
//            for (short j = 0; j < entrys1.size(); ++j) {
//                if (entrys1.get(j).nclass == 7) {
//                    m.writer().writeByte(entrys1.get(j).id);
//                    m.writer().writeUTF(entrys1.get(j).name);
//                    m.writer().writeByte(entrys1.get(j).maxPoint);
//                    m.writer().writeByte(entrys1.get(j).type);
//                    m.writer().writeShort(entrys1.get(j).iconId);
//                    m.writer().writeUTF(entrys1.get(j).desc);
//                    m.writer().writeByte(entrys1.get(j).templates.size());
//                    //System.out.println("SkillData.entrys.get(j).templates.size()" + SkillData.entrys.get(j).templates.size());
//                    for (int k = 0; k < entrys1.get(j).templates.size(); k++) {
//                        //System.out.println("SkillData.entrys.get(j).templates.get(k)" + SkillData.entrys.get(j).templates.toString());
//                        m.writer().writeShort(entrys1.get(j).templates.get(k).skillId);
//                        m.writer().writeByte(entrys1.get(j).templates.get(k).point);
//                        m.writer().writeByte(entrys1.get(j).templates.get(k).level);
//                        m.writer().writeShort(entrys1.get(j).templates.get(k).manaUse);
//                        m.writer().writeInt(entrys1.get(j).templates.get(k).coolDown);
//                        m.writer().writeShort(entrys1.get(j).templates.get(k).dx);
//                        m.writer().writeShort(entrys1.get(j).templates.get(k).dy);
//                        m.writer().writeByte(entrys1.get(j).templates.get(k).maxFight);
//                        m.writer().writeByte(entrys1.get(j).templates.get(k).options.size());
//                        for (int l = 0; l < entrys1.get(j).templates.get(k).options.size(); l++) {
//                            //System.out.println("SkillData.entrys.get(j).templates.get(k).options.size()" + SkillData.entrys.get(j).templates.get(k).options.toString());
//                            m.writer().writeShort(entrys1.get(j).templates.get(k).options.get(l).param);
//                            m.writer().writeByte(entrys1.get(j).templates.get(k).options.get(l).id);
//                        }
//                    }
//                }
//            }
            m.writer().flush();
            p.session.sendMessage(m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (m != null) {
                m.cleanup();
            }
        }
    }

    /*public void sendItem(final User p) throws IOException {
        final Message m = new Message(-28);
        m.writer().writeByte(-119);
        m.writer().write(cache[3].toByteArray());
        m.writer().flush();
        p.sendMessage(m);
        m.cleanup();

    }*/
    public void createItem1(final User p) {
        Message m = new Message(-28);
        try {
            m.writer().writeByte(-119);
            m.writer().writeByte(vsItem);
            m.writer().writeByte(Manager.iOptionTemplates.length);
            for (short i = 0; i < Manager.iOptionTemplates.length; ++i) {
                m.writer().writeUTF(Manager.iOptionTemplates[i].name);
                m.writer().writeByte(Manager.iOptionTemplates[i].type);
            }
            m.writer().writeShort(Manager.itemTemplates.length);
            for (short j = 0; j < Manager.itemTemplates.length; ++j) {
                m.writer().writeByte(Manager.itemTemplates[j].type);
                m.writer().writeByte(Manager.itemTemplates[j].gender);
                m.writer().writeUTF(Manager.itemTemplates[j].name);
                m.writer().writeUTF(Manager.itemTemplates[j].description);
                m.writer().writeByte(Manager.itemTemplates[j].level);
                m.writer().writeShort(Manager.itemTemplates[j].iconID);
                m.writer().writeShort(Manager.itemTemplates[j].part);
                m.writer().writeBoolean(Manager.itemTemplates[j].isUpToUp);
            }
            m.writer().flush();
            p.sendMessage(m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (m != null) {
                m.cleanup();
            }
        }
    }

    public void sendItem(final User p) throws IOException {
//        final Message m = new Message(-28);
//        m.writer().writeByte(-119);
//        m.writer().write(cache[3].toByteArray());
//        m.writer().flush();
//        p.sendMessage(m);
//        m.cleanup();

        Message m = new Message(-28);
        try {
            m.writer().writeByte(-119);
            m.writer().writeByte(this.vsItem);
            //System.out.println("Length: "+Manager.iOptionTemplates.length);
            // for(int k=0;k<Manager.iOptionTemplates.length;k++){
            //     System.out.println(Manager.iOptionTemplates[k].toString());        
            // }
                
            m.writer().writeByte(Manager.iOptionTemplates.length);
            for (short i = 0; i < Manager.iOptionTemplates.length; ++i) {
                //System.out.println(Manager.iOptionTemplates.toString());        
                m.writer().writeUTF(Manager.iOptionTemplates[i].name);
                m.writer().writeByte(Manager.iOptionTemplates[i].type);
            }
            m.writer().writeShort(ItemData.entrys.size());
            for (int j = 0; j < ItemData.entrys.size(); ++j) {
                m.writer().writeByte(ItemData.entrys.get(j).type);
                m.writer().writeByte(ItemData.entrys.get(j).gender);
                m.writer().writeUTF(ItemData.entrys.get(j).name);
                m.writer().writeUTF(ItemData.entrys.get(j).description);
                m.writer().writeByte(ItemData.entrys.get(j).level);
                m.writer().writeShort(ItemData.entrys.get(j).iconID);
                m.writer().writeShort(ItemData.entrys.get(j).part);
                m.writer().writeBoolean(ItemData.entrys.get(j).isUpToUp);
            }
            m.writer().flush();
            p.sendMessage(m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (m != null) {
                m.cleanup();
            }
        }

    }
}
