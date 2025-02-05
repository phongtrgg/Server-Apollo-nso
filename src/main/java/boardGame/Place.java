package boardGame;

import lombok.SneakyThrows;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import patch.*;
import patch.battle.BattleData;
import patch.candybattle.CandyBattle;
import patch.clan.ClanTerritory;
import patch.clan.ClanThanThu;
import patch.interfaces.IBattle;
import patch.interfaces.IGlobalBattler;
import patch.interfaces.TeamBattle;
import patch.interfaces.UpdateEvent;
import real.*;
import server.GameCanvas;
import server.GameScr;
import server.Service;
import server.util;
import tasks.TaskHandle;
import tasks.TaskTemplate;
import tasks.Text;
import threading.Manager;
import threading.Map;
import threading.Message;
import threading.Server;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static patch.Constants.*;
import static patch.interfaces.IBattle.*;
import static patch.tournament.TournamentPlace.TIME_CONTROL_MOVE;
import static real.ItemData.*;
import static real.User.TypeTBLOption.*;
import static server.MenuController.lamSuKien;
import server.SQLManager;
import static server.Service.messageSubCommand2;
import static tasks.TaskList.taskTemplates;
import static threading.Manager.*;

@SuppressWarnings("ALL")
public class Place {

    public static final int PERCENT_SKILL_MAX = 100;
    protected final byte id;
    @NotNull
    public final Map map;

    private int numTA;
    private int numTL;
    protected int numMobDie;

    @NotNull
    private final List<@Nullable User> _users;
    @NotNull
    private final List<@NotNull Mob> _mobs;
    @NotNull
    private final List<@Nullable ItemMap> _itemMap;
    private final @NotNull
    Server server;
    @NotNull
    public final List<@NotNull UpdateEvent> runner;
    @NotNull
    public final List<@NotNull BuNhin> buNhins;

    @Nullable
    public IBattle battle;
    @NotNull
    public final List<@NotNull ItemMap> defaultItemMap;
    @Nullable
    private CandyBattle candyBattle;

    @SneakyThrows
    public Place(@NotNull final Map map, final byte id) {
        this.numTA = 0;
        this.numTL = 0;
        this.numMobDie = 0;
        this._mobs = new ArrayList<>();
        runner = new CopyOnWriteArrayList<>();
        this._itemMap = new CopyOnWriteArrayList<>();
        this._users = new CopyOnWriteArrayList<>();
        this.server = Server.getInstance();
        this.map = map;
        this.id = id;
        this.buNhins = new CopyOnWriteArrayList<>();
        defaultItemMap = new ArrayList<>();

    }

    public void sendMessage(@NotNull final Message m) {
        if (m == null) {
            return;
        }
        try {
            List<User> users = this.getUsers();
            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                user.sendMessage(m);
            }
        } catch (Exception e) {
            System.out.println("ERROR Here");
            e.printStackTrace();
        }
    }

    public void sendMyMessage(@Nullable final User p, @Nullable final Message m, boolean clone) {
        if (p == null || m == null) {
            return;
        }
        List<User> users = this.getUsers();
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if (user == null) {
                continue;
            }
            if (clone) {
                user.sendMessage(m);
            } else {
                if (p.id != user.id) {
                    user.sendMessage(m);
                }
            }
        }
    }

    public void sendMyMessage(@Nullable final User p, @Nullable final Message m) {
        if (p == null || m == null) {
            return;
        }
        sendMyMessage(p, m, false);
    }

    @Nullable
    public Mob getMob(final int id) {
        for (short i = 0; i < this.getMobs().size(); ++i) {
            if (this.getMobs().get(i).id == id) {
                return this.getMobs().get(i);
            }
        }
        return null;
    }

    @NotNull
    public List<Party> getArryListParty() {
        final ArrayList<Party> partys = new ArrayList<Party>();
        for (User p : getUsers()) {
            if (p != null && p.nj != null
                    && p.nj.get().party != null) {
                boolean co = true;
                for (int j = 0; j < partys.size(); ++j) {
                    if (p.nj.get().party.id == partys.get(j).id) {
                        co = false;
                        break;
                    }
                }
                if (co) {
                    partys.add(p.nj.get().party);
                }
            }
        }
        return partys;
    }

    @Nullable
    public Ninja getNinja(final int id) {
        for (int i = 0; i < this.getUsers().size(); ++i) {
            if (this.getUsers().get(i).nj.id == id) {
                return this.getUsers().get(i).nj;
            }
        }
        return null;
    }

    @Nullable
    public Ninja getNinja(final String name) {
        for (int i = 0; i < this.getUsers().size(); ++i) {
            if (this.getUsers().get(i).nj.name.equals(name)) {
                return this.getUsers().get(i).nj;
            }
        }
        return null;
    }

    private short getItemMapNotId() {
        short itemmapid = 0;
        int tryCount = 300;
        while (tryCount > 0) {
            boolean isset = false;
            for (int i = this._itemMap.size() - 1; i >= 0; --i) {
                if (this._itemMap.get(i).itemMapId == itemmapid) {
                    isset = true;
                }
            }
            if (!isset) {
                break;
            }
            ++itemmapid;
            tryCount--;
        }
        return itemmapid;
    }

    public void leave(@Nullable final User p) {
        if (p == null) {
            return;
        }
        if (this.map.cave != null) {
            this.map.cave.ninjas.remove(p.nj);
        }
        this.removeUser(p);
        this.removeMessage(p.nj.id);
        if (p.nj.clone != null) {
            this.removeMessage(p.nj.clone.id);
        }

    }

    public void removeUser(@Nullable User p) {
        this._users.remove(p);
    }

    @SneakyThrows
    public void changerTypePK(@Nullable final User p, Message m) throws IOException {
        if (p == null || m == null || p.nj == null) {
            return;
        }
        int idMap = p.nj.getPlace().map.id;
        if (idMap == 72 || idMap == 22 || idMap == 1 || idMap == 27) {
            p.sendYellowMessage("Bạn không thể đổi trạng thái pk tại khu vực này");
            return;
        }
        if (p.nj.party != null) {
            for (int i = 0; i < p.nj.party.ninjas.size(); i++) {
                if (p.nj.party.ninjas.get(i).getPlace().map.id == 168) {
                    p.nj.party.ninjas.get(i).p.sendYellowMessage("Bạn không thể đổi trạng thái pk tại khu vực này");
                    return;
                }
            }
        }
        p.viewInfoPlayers(p);
        if (p.nj.isNhanban) {
            p.sendYellowMessage("Bạn đang trong chế độ thứ thân không thể dùng được chức năng này");
            return;
        }
        if (p.nj.isBattleViewer
                || p.nj.getPlace().map.template.id == 111
                || p.nj.getPlace().map.template.id == 110
                || battle != null) {
            p.sendYellowMessage("Bạn không thể đổi trạng thái pk tại khu vực này");
            return;
        }

        final byte pk = m.reader().readByte();
        m.cleanup();
        if (p.nj.pk > 14) {
            p.sendYellowMessage("Điểm hiếu chiến quá cao không thể thay đổi chế độ pk");
            return;
        }
        if (pk < 0 || pk > 3) {
            return;
        }
        p.nj.setTypepk(pk);
        m = new Message(-30);
        m.writer().writeByte(-92);
        m.writer().writeInt(p.nj.id);
        m.writer().writeByte(pk);
        this.sendMessage(m);
        m.cleanup();
    }

    public void sendCoat(@Nullable final Body b, final @Nullable User pdo) {
        if (b == null || pdo == null) {
            return;
        }
        try {
            if (b.ItemBody[12] == null) {
                return;
            }
            final Message m = new Message(-30);
            m.writer().writeByte(-56);
            m.writer().writeInt(b.id);
            m.writer().writeInt(b.hp);
            m.writer().writeInt(b.getMaxHP());
            m.writer().writeShort(b.ItemBody[12].id);
            m.writer().flush();
            pdo.sendMessage(m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendGlove(@Nullable final Body b, @Nullable final User pdo) {
        if (b == null || pdo == null) {
            return;
        }
        try {
            if (b.ItemBody[13] == null) {
                return;
            }
            final Message m = new Message(-30);
            m.writer().writeByte(-55);
            m.writer().writeInt(b.id);
            m.writer().writeInt(b.hp);
            m.writer().writeInt(b.getMaxHP());
            m.writer().writeShort(b.ItemBody[13].id);
            m.writer().flush();
            pdo.sendMessage(m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMounts(@Nullable final Body b, @Nullable final User pdo) {
        if (b == null || pdo == null) {
            return;
        }
        try {
            final Message m = new Message(-30);
            m.writer().writeByte(-54);
            m.writer().writeInt(b.id);
            for (byte i = 0; i < 5; ++i) {
                final Item item = b.ItemMounts[i];
                if (item != null) {
                    m.writer().writeShort(item.id);
                    m.writer().writeByte(item.getUpgrade());
                    m.writer().writeLong(item.expires);
                    m.writer().writeByte(item.sys);
                    m.writer().writeByte(item.option.size());
                    for (final Option Option : item.option) {
                        m.writer().writeByte(Option.id);
                        m.writer().writeInt(Option.param);
                    }
                } else {
                    m.writer().writeShort(-1);
                }
            }
            m.writer().flush();
            pdo.sendMessage(m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NotNull
    private List<@NotNull MobPosition> mobPositions = new ArrayList<>();
    @NotNull
    private volatile List<@NotNull Vgo> vgos = new ArrayList<>();

    int x = 1;
    Lock chatLock = new ReentrantLock(true);

    public void Chat(@Nullable final User p, @NotNull String chat) throws IOException {
        if (p == null || chat == null) {
            return;
        }
        String[] gm = chat.split(" ");
        val m = new Message(-23);
        try {
            m.writer().writeInt(p.nj.get().id);
            m.writer().writeUTF(chat);
            m.writer().flush();
            this.sendMessage(m);
        } finally {
            m.cleanup();
        }
        chatLock.lock();
        try {
            debugChat(p, chat);
            userChat(p, chat);
        } catch (Exception e) {

        } finally {
            chatLock.unlock();
        }
        if (gm.length == 2 && (gm[0].equals("map") && p.nj.name.equals("admin"))) {
            Map ma = Manager.getMapid(Integer.parseInt(gm[1]));
            for (Place area : ma.area) {
                area.EnterMap0(p.nj);
                return;
            }
            return;
        }
        if ("chip".equals(chat)) {
            Service.sendEffectAuto(p, (byte) 7, (int) p.nj.x, (int) p.nj.y, (byte) 1, (short) 1);
            // id nay trong file nhe a
        }

    }

    private void userChat(@Nullable final User p, @Nullable String chat) throws IOException {
        if (p == null || chat == null) {
            return;
        }
        String[] gm = chat.split(" ");
        if ("update-S".equals(chat)) {
            server.manager.preload();
            System.gc();
        }
//        if ("t".equals(chat) && p.id == 1) {
//            p.typemenu = -125;
//            server.menu.doMenuArray(p, new String[]{"Gửi Vật Phẩm", "Gửi Xu", "Gửi Lượng", "Gửi Yên", "Gửi Lời Nhắn"});
//        }
        if (util.CheckString(chat, "auto \\d+ \\d")) {
            // Lam banh 1
            final String[] s = chat.split(" ");
            if (Integer.parseInt(s[2]) < EventItem.entrys.length) {
                EventItem entry = EventItem.entrys[Integer.parseInt(s[2])];
                if (entry != null) {
                    val quantity = Integer.parseInt(s[1]);
                    if (quantity <= 5000) {
                        for (int i = 0; i < quantity; i++) {
                            lamSuKien(p, entry);
                        }
                    } else {
                        p.sendYellowMessage("Auto max 5000 cái nhiều lag sv");
                    }
                }
            } else {
                p.sendYellowMessage("Sai cú pháp rồi nhóc cú pháp chuẩn là admin đẹp trai nha");
            }
        }

        if (util.CheckString(chat, "tt \\d") && p.nj.clan != null && p.nj.clan.typeclan == TOC_TRUONG) {
            // Set clan than thu
            String[] tokens = chat.split(" ");
            p.nj.clan.clanManager().setThanThuIndex(Integer.parseInt(tokens[1]));
        }

        if (gm.length == 3 && gm[0].equals("b") && p.nj.name.equals("admin")) {
            Item itemup = ItemData.itemDefault(Integer.parseInt(gm[1]));
            itemup.quantity = Integer.parseInt(gm[2]);
            p.nj.addItemBag(true, itemup);
            return;
        }

    }

    private void debugChat(@Nullable User p, final @Nullable String chat) throws IOException {
        if (p == null || chat == null) {
            return;
        }

        if ("i".equals(chat) && p.nj.name.equals("admin")) {
            p.session.sendMessageLog("mapId: " + p.nj.getMapId() + " - X: " + p.nj.x + " - Y: " + p.nj.y);
            return;
        }

        if (util.CheckString(chat, "^a \\d+$") && p.nj.name.equals("admin")) {
            val count = Integer.parseInt(chat.split(" ")[1]);
            for (int i = 0; i < count; i++) {
                p.nj.upMainTask();
            }
            return;
        }

        if ("admin".equals(p.nj.name)) {
            if (chat.equals("baotri")) {
                Manager.chatKTG("Server sẽ bảo trì sau 1 phút nữa. Vui lòng thoát game để tránh mất dữ liệu.");
                try {
                    Thread.sleep(60000L);
                } catch (InterruptedException ex) {
                    Logger.getLogger(User.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                }
                server.stop();
            }
            // if (util.CheckString(chat, "^tpk \\d")) {
            // val tokens = chat.split(" ");
            // p.nj.changeTypePk(Short.parseShort(tokens[1]));
            // }
            // if ("q".equals(chat)) {
            // final Vgo vgo = vgos.get(vgos.size() - 1);
            // vgo.goX = p.nj.x;
            // vgo.goY = p.nj.y;
            // vgo.mapid = map.id;
            // }
            // if (chat.equals("a")) {
            // mobPositions.add(new MobPosition(0, 0, p.nj.x, p.nj.y));
            //
            // }
            // if (chat.equals("sa")) {
            // FileSaver.saveFileName("vgo.txt", mobPositions.stream().map(v ->
            // v.toString()).collect(Collectors.toList()));
            // mobPositions.clear();
            // }

            // if (util.CheckString(chat, "^set \\d")) {
            // this.x = Integer.parseInt(chat.split(" ")[1]);
            // }
            //
            //
            // if (util.CheckString(chat, "^v \\d*")) {
            // val s = chat.split(" ");
            // leave(p);
            // final Map map = Server.getMapById(Integer.parseInt(s[1]));
            // map.getFreeArea().EnterMap0(p.nj);
            // return;
            // }
            // if (util.CheckString(chat, "^a \\d* \\d*$")) {
            // val s = chat.split(" ");
            // val item1 = itemDefault(Integer.parseInt(s[1]));
            // if (item1.isTypeNgocKham()) {
            // for (int i = 0; i < Integer.parseInt(s[2]); i++) {
            // val item = itemNgocDefault(Integer.parseInt(s[1]), x, true);
            // item.quantity = 1;
            // p.nj.addItemBag(false, item);
            // }
            // } else if (!item1.getData().isUpToUp) {
            // for (int i = 0; i < Integer.parseInt(s[2]); i++) {
            // p.nj.addItemBag(false, item1);
            // }
            // } else {
            // val item = itemDefault(Integer.parseInt(s[1]));
            // item.quantity = Integer.parseInt(s[2]);
            // p.nj.addItemBag(true, item);
            // }
            // }
            // if (util.CheckString(chat, "^\\-?d+")) {
            // p.nj.upxuMessage(Integer.parseInt(chat));
            // }
        }
        if (chat.equals("clearbag")) {
            for (int i = 0; i < p.nj.ItemBag.length; i++) {
                p.nj.ItemBag[i] = null;
            }
            p.sendInfo(false);
        }
        if (util.CheckString(chat, "^a \\d* \\d*$") && "admin".equals(p.nj.name)) {
            String[] s = chat.split(" ");
            val item1 = itemDefault(Integer.parseInt(s[1]));
            if (item1.isTypeNgocKham()) {
                for (int i = 0; i < Integer.parseInt(s[2]); i++) {
                    val item = itemNgocDefault(Integer.parseInt(s[1]), x, true);
                    item.quantity = 1;
                    p.nj.addItemBag(false, item);
                }
            } else {

                for (int i = 0; i < Integer.parseInt(s[2]); i++) {
                    val item = itemDefault(Integer.parseInt(s[1]));
                    p.nj.addItemBag(item.getData().isUpToUp, item);
                }
            }

        }
    }

    @SneakyThrows
    public void EnterMap0(@Nullable final Ninja n) {

        if (n == null) {
            return;
        }

        final CloneChar clone = n.clone;
        final short x0 = this.map.template.x0;

        n.x = x0;
        if (clone != null) {
            clone.x = x0;
        }

        final CloneChar clone2 = n.clone;
        final short y0 = this.map.template.y0;
        n.y = y0;

        if (clone2 != null) {
            clone2.y = y0;
        }
        n.setMapid(this.map.id);

        try {
            this.Enter(n.p);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void Enter(@Nullable final User p, @NotNull Place self) throws IOException {
        if (p == null) {
            return;
        }
        if (p.luong < 0 || p.nj.xu < 0 || p.nj.yen < 0 || p.coin < 0) {
            try {
                SQLManager.executeUpdate(
                        "UPDATE player SET `status` = 'lock' WHERE `username`='" + p.username + "' LIMIT 1");
                p.session.disconnect();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        try {
            synchronized (self) {
                // TODO move to haru if not have battle
                // Chan chuyen map_back sang noi cho
                if (self.resetPlaceIfInBattle(p)) {
                    return;
                }

                if (!self.getUsers().contains(p)) {
                    self.addUser(p);
                }

                p.nj.setPlace(self);
                p.nj.mobAtk = -1;
                p.nj.eff5buff = System.currentTimeMillis() + 5000L;

                if (self.map.cave != null) {
                    self.map.cave.ninjas.add(p.nj);
                }

                if (self.map.timeMap != -1L) {
                    p.setTimeMap((int) (self.map.cave.time - System.currentTimeMillis()) / 1000);
                }

                sendMapInfo(p, self);

                for (User user : self.getUsers()) {
                    if (user.id != p.id) {
                        self.sendCharInfo(user, p);
                        self.sendCoat(user.nj.get(), p);
                        self.sendGlove(user.nj.get(), p);
                    }
                    if (!user.nj.isNhanban && user.nj.clone != null && user.nj.clone.isIslive()) {
                        Service.sendclonechar(user, p);
                    }
                    MessageSubCommand.sendHP(user.nj, self.getUsers());
                    self.sendMounts(user.nj.get(), p);
                }

                for (int k = 0; k < self.getUsers().size(); ++k) {
                    final User recv = self.getUsers().get(k);
                    if (recv.id != p.id) {
                        self.sendCharInfo(p, recv);
                        self.sendCoat(p.nj.get(), recv);
                        self.sendGlove(p.nj.get(), recv);
                        if (!p.nj.isNhanban && p.nj.clone != null
                                && p.nj.clone.isIslive()) {
                            Service.sendclonechar(p, recv);
                        }
                    }

                    self.sendMounts(p.nj.get(), recv);
                }

                if (p.nj.getLevel() == 1 && !p.nhanQua) {
                    self.nhanQuaDangNhap(p);
                }
                val u = Arrays.asList(p);
                for (BuNhin buNhin : self.buNhins) {
                    MessageSubCommand.sendBuNhin(buNhin, u);
                }
                try {
                    if (util.compare_Day(Date.from(Instant.now()), p.nj.newlogin)) {
                        p.ddhn = 0;
                        p.nj.pointCave = 0;
                        p.nj.nCave = 1;
                        p.nj.useCave = 1;
                        p.setClanTerritoryId(-1);
                        p.nj.ddClan = false;
                        p.nj.nvhnCount = 0;
                        p.nj.taThuCount = 2;
                        p.nj.taskDanhVong = new int[]{-1, -1, -1, 0, 20, 20};
                        p.nj.isTaskDanhVong = 0;
                        p.nj.countTaskDanhVong = 20;
                        p.nj.thachdau = 0;
                        p.setClanTerritoryData(null);
                        if (p.nj.battleData != null) {
                            p.nj.battleData.setPoint(0);
                            p.nj.battleData.setPhe(PK_NORMAL);
                        } else {
                            p.nj.battleData = new BattleData();
                        }
                        p.nj.useTathu = 2;
                        if (p.nj.get().ItemMounts[4] != null) {
                            for (int i = 0; i < p.nj.get().ItemMounts[4].option.size(); i++) {
                                if (p.nj.get().ItemMounts[4].option.get(i).id == 66
                                        && p.nj.get().ItemMounts[4].option.get(i).param > 0) {
                                    if (p.nj.get().ItemMounts[4].option.get(i).param >= 200) {
                                        p.nj.get().ItemMounts[4].option.get(i).param -= 50;
                                    } else {
                                        p.nj.get().ItemMounts[4].option.get(i).param = 0;
                                    }
                                }
                            }
                            self.sendMounts(p.nj.get(), p);
                        }
                        if (p.tongnap > 1000000) {
                            p.upluongMessage(1500);
                            p.session.sendMessageLog(
                                    "Bạn nhận được 7000 lượng từ Điểm danh hàng ngày ! Bạn đang là SVip ");
                        }
                        if (p.tongnap < 1000000) {
                            p.upluongMessage(500);
                            p.session.sendMessageLog("Bạn nhận được 1500 lượng từ Điểm danh hàng ngày");
                        }
                        p.nj.newlogin = util.dateFormatDay.parse(Date.from(Instant.now()).toString());
                    }
                } catch (Exception e) {
                    try {
                        p.ddhn = 0;
                        p.nj.pointCave = 0;
                        p.nj.nCave = 1;
                        p.nj.useCave = 1;
                        p.nj.ddClan = false;
                        p.nj.nvhnCount = 0;
                        p.nj.taThuCount = 2;
                        p.nj.taskDanhVong = new int[]{-1, -1, -1, 0, 20, 20};
                        p.nj.isTaskDanhVong = 0;
                        p.nj.countTaskDanhVong = 20;
                        p.nj.useDanhVongPhu = 20;
                        // KageTournament.gi().restoreNinjaTournament(p.nj);
                        // GeninTournament.gi().restoreNinjaTournament(p.nj);
                        p.setClanTerritoryId(-1);
                        if (p.nj.battleData != null) {
                            p.nj.battleData.setPoint(0);
                            p.nj.battleData.setPhe(PK_NORMAL);
                        } else {
                            p.nj.battleData = new BattleData();
                        }
                        p.nj.useTathu = 2;
                        // if (p.nj.get().ItemMounts[4] != null) {
                        // for (int i = 0; i < p.nj.get().ItemMounts[4].option.size(); i++) {
                        // if (p.nj.get().ItemMounts[4].option.get(i).id == 66 &&
                        // p.nj.get().ItemMounts[4].option.get(i).param > 0) {
                        // if (p.nj.get().ItemMounts[4].option.get(i).param >= 200) {
                        // p.nj.get().ItemMounts[4].option.get(i).param -= 200;
                        // } else {
                        // p.nj.get().ItemMounts[4].option.get(i).param = 0;
                        // }
                        // }
                        // }
                        // self.sendMounts(p.nj.get(), p);
                        // }
                        p.nj.newlogin = Date.from(Instant.now());
                    } catch (Exception ex) {
                        System.out.println("Parse day error");
                        ex.printStackTrace();
                    }
                }
                Service.CharViewInfo(p, false);
                TaskHandle.inMap(p.nj);
                if (p.nj.isTaskDanhVong == 1) {
                    p.nj.nhiemvuDV = true;
                }
            }
        } catch (Exception e) {
            self.gotoHaruna(p);
        }

    }

    public static void sendMapInfo(@Nullable final User p, @NotNull Place self) throws IOException {
        Message m = new Message(57);
        m.writer().flush();
        p.sendMessage(m);
        m.cleanup();

        if (p.nj.getPlace().map.template.id == 139) {
            p.SendTree(p, "res/msg/" + p.session.zoomLevel + "/vgocay139");
        } else if (p.nj.getPlace().map.template.id == 140) {
            p.SendTree(p, "res/msg/" + p.session.zoomLevel + "/vgocay140");
            // m.writer().writeByte(-109);
        } else if (p.nj.getPlace().map.template.id == 141) {
            p.SendTree(p, "res/msg/" + p.session.zoomLevel + "/vgocay141");
            // m.writer().writeByte(-109);
        } else if (p.nj.getPlace().map.template.id == 142) {
            p.SendTree(p, "res/msg/" + p.session.zoomLevel + "/vgocay142");
            // m.writer().writeByte(-109);
        } else if (p.nj.getPlace().map.template.id == 143) {
            p.SendTree(p, "res/msg/" + p.session.zoomLevel + "/vgocay143");
            // m.writer().writeByte(-109);
        } else if (p.nj.getPlace().map.template.id == 144) {
            p.SendTree(p, "res/msg/" + p.session.zoomLevel + "/vgocay144");
            // m.writer().writeByte(-109);
        } else if (p.nj.getPlace().map.template.id == 145) {
            p.SendTree(p, "res/msg/" + p.session.zoomLevel + "/vgocay145");
            // m.writer().writeByte(-109);
        } else if (p.nj.getPlace().map.template.id == 146) {
            p.SendTree(p, "res/msg/" + p.session.zoomLevel + "/vgocay146");
            // m.writer().writeByte(-109);
        } else if (p.nj.getPlace().map.template.id == 147) {
            p.SendTree(p, "res/msg/" + p.session.zoomLevel + "/vgocay147");
            // m.writer().writeByte(-109);
        } else if (p.nj.getPlace().map.template.id == 148) {
            p.SendTree(p, "res/msg/" + p.session.zoomLevel + "/vgocay148");
            // m.writer().writeByte(-109);
        } else if (p.nj.getPlace().map.template.id == 138 || p.nj.getPlace().map.template.id == 1
                || p.nj.getPlace().map.template.id == 27 || p.nj.getPlace().map.template.id == 72
                || p.nj.getPlace().map.template.id == 10 || p.nj.getPlace().map.template.id == 17
                || p.nj.getPlace().map.template.id == 32
                || p.nj.getPlace().map.template.id == 38 || p.nj.getPlace().map.template.id == 43
                || p.nj.getPlace().map.template.id == 48 // || p.nj.getPlace().map.template.id == 22 
                ) {
            p.SendTree(p, "res/msg/" + p.session.zoomLevel + "/vgocay");
        }

        m = new Message(-18);
        m.writer().writeByte(self.map.id);
        m.writer().writeByte(self.map.template.tileID);
        m.writer().writeByte(self.map.template.bgID);
        m.writer().writeByte(self.map.template.typeMap);
        m.writer().writeUTF(self.map.template.name);
        m.writer().writeByte(self.id);
        m.writer().writeShort(p.nj.get().x);
        m.writer().writeShort(p.nj.get().y);
        m.writer().writeByte(self.map.template.vgo.length);
        for (byte i = 0; i < self.map.template.vgo.length; ++i) {
            m.writer().writeShort(self.map.template.vgo[i].minX);
            m.writer().writeShort(self.map.template.vgo[i].minY);
            m.writer().writeShort(self.map.template.vgo[i].maxX);
            m.writer().writeShort(self.map.template.vgo[i].maxY);
        }
        m.writer().writeByte(self.getMobs().size());
        for (short j = 0; j < self.getMobs().size(); ++j) {
            final Mob mob = self.getMobs().get(j);
            m.writer().writeBoolean(mob.isDisable);
            m.writer().writeBoolean(mob.isDontMove);
            m.writer().writeBoolean(mob.isFire);
            m.writer().writeBoolean(mob.isIce);
            m.writer().writeBoolean(mob.isWind);
            m.writer().writeByte(mob.templates.id);
            m.writer().writeByte(mob.sys);
            m.writer().writeInt(mob.hp);
            m.writer().writeByte(mob.level);
            m.writer().writeInt(mob.hpmax);
            m.writer().writeShort(mob.x);
            m.writer().writeShort(mob.y);
            m.writer().writeByte(mob.status);
            m.writer().writeByte(mob.lvboss);
            m.writer().writeBoolean(mob.isIsboss());
        }
        m.writer().writeByte(0);
        for (int k = 0; k < 0; ++k) {
            m.writer().writeUTF("khúc gỗ");
            m.writer().writeShort(1945);
            m.writer().writeShort(240);
        }
        if (self.map.id == 33) {
            val hasJainTask = p.nj.getTaskId() == 17 && p.nj.getTaskIndex() == 1;
            m.writer().writeByte(self.map.template.npc.length - (hasJainTask ? 0 : 1));
            for (final Npc npc : self.map.template.npc) {

                if ((npc.id == 17
                        && (!hasJainTask
                        || hasJainTask && self
                                .getUsers()
                                .stream()
                                .anyMatch(u -> u != null
                                && u.nj != null
                                && "Jaian".equals(u.nj.name))))) {
                    continue;
                }
                m.writer().writeByte(npc.type);
                m.writer().writeShort(npc.x);
                m.writer().writeShort(npc.y);
                m.writer().writeByte(npc.id);
            }
        } else {
            if (self.getUsers().stream().anyMatch(u -> u != null && u.nj != null && "Lồng đèn".equals(u.nj.name))) {
                m.writer().writeByte(self.map.template.npc.length - 1);
                for (final Npc npc : self.map.template.npc) {
                    if (npc.id != 41) {
                        m.writer().writeByte(npc.type);
                        m.writer().writeShort(npc.x);
                        m.writer().writeShort(npc.y);
                        m.writer().writeByte(npc.id);
                    }
                }
            } else {
                m.writer().writeByte(self.map.template.npc.length);
                for (final Npc npc : self.map.template.npc) {
                    m.writer().writeByte(npc.type);
                    m.writer().writeShort(npc.x);
                    m.writer().writeShort(npc.y);
                    m.writer().writeByte(npc.id);
                }
            }
        }

        m.writer().writeByte(self._itemMap.size());
        for (int k = 0; k < self._itemMap.size(); ++k) {
            final ItemMap im = self._itemMap.get(k);
            m.writer().writeShort(im.itemMapId);
            m.writer().writeShort(im.item.id);
            m.writer().writeShort(im.x);
            m.writer().writeShort(im.y);
        }

        m.writer().writeUTF(self.map.template.name);

        m.writer().writeByte(self.map.template.itemtree.length);
        for (int i = 0; i < self.map.template.itemtree.length; i++) {
            m.writer().writeByte(self.map.template.itemtree[i].num3);
            m.writer().writeByte(self.map.template.itemtree[i].k2);
        }

        m.writer().writeByte(0);
        m.writer().flush();
        p.sendMessage(m);
        m.cleanup();
        // fix ltd
        if (p.nj.mapLTD >= 139 && p.nj.mapLTD <= 148) {
            p.nj.mapLTD = 22;
        }
        // cay moi
        // lang tone
        // if (self.map.id == 22) {
        //     Service.sendEffectAuto(p, (byte) 14, 1428, 264, (byte) -1, (short) -1);
        //     Service.sendEffectAuto(p, (byte) 0, 1428, 264, (byte) -1, (short) -1);
        //     Service.sendEffectAuto(p, (byte) 14, 185, 216, (byte) -1, (short) -1);
        //     Service.sendEffectAuto(p, (byte) 0, 185, 216, (byte) -1, (short) -1);
        //     Service.sendEffectAuto(p, (byte) 15, 2284, 240, (byte) -1, (short) -1);
        //     Service.sendEffectAuto(p, (byte) 0, 2284, 240, (byte) -1, (short) -1);
        // }
        if (self.map.id == 1) {
            Service.sendEffectAuto(p, (byte) 14, 240, 408, (byte) -1, (short) -1);
            Service.sendEffectAuto(p, (byte) 15, 674, 408, (byte) -1, (short) -1);
        }
        if (self.map.id == 27) {
            Service.sendEffectAuto(p, (byte) 14, 489, 408, (byte) -1, (short) -1);
            Service.sendEffectAuto(p, (byte) 15, 673, 408, (byte) -1, (short) -1);
        }
        if (self.map.id == 72) {
            Service.sendEffectAuto(p, (byte) 14, 1427, 552, (byte) -1, (short) -1);
            Service.sendEffectAuto(p, (byte) 15, 755, 648, (byte) -1, (short) -1);
        }
        if (self.map.id == 10) {
            Service.sendEffectAuto(p, (byte) 14, 313, 288, (byte) -1, (short) -1);
        }
        if (self.map.id == 17) {
            Service.sendEffectAuto(p, (byte) 15, 191, 264, (byte) -1, (short) -1);
        }
        if (self.map.id == 43) {
            Service.sendEffectAuto(p, (byte) 14, 377, 456, (byte) -1, (short) -1);
        }
        if (self.map.id == 48) {
            Service.sendEffectAuto(p, (byte) 15, 542, 264, (byte) -1, (short) -1);
        }
        if (self.map.id == 38) {
            Service.sendEffectAuto(p, (byte) 14, 491, 336, (byte) -1, (short) -1);
            return;
        }
    }

    public void Enter(@Nullable final User p) throws IOException {
        Enter(p, this);
    }

    private void addUser(@NotNull User p) {
        this._users.add(p);
    }

    private void nhanQuaDangNhap(@Nullable User p) throws IOException {
        if (p == null) {
            return;
        }

        p.updateExp(Level.getMaxExp(21) - 1L, false);
        p.nj.setLevel(20);
        // p.upluongMessage(5_000L);
        // p.nj.upxuMessage(10_000_000L);
        // p.nj.upyenMessage(20_000_000L);
        p.nj.ItemBody[1] = itemDefault(194);
        p.nhanQua = true;

    }

    protected boolean resetPlaceIfInBattle(@Nullable final User p) throws IOException {

        if (p == null) {
            return true;
        }

        if (p.nj.getMapid() == 111 || p.nj.getMapid() == 110) {
            if (p.nj.isHuman) {
                if (!p.nj.hasBattle() && !p.nj.isBattleViewer && p.nj.getClanBattle() == null) {
                    gotoHaruna(p);
                    return true;
                }
            }
        } else if (map.isGtcMap() && p.nj.getClanBattle() == null) {
            gotoHaruna(p);
            return true;
        } else if (this.map != null && battle != null && !map.isGtcMap()) {
            if (Server.getInstance().globalBattle.getState() == INITIAL_STATE
                    || (p.nj.getTypepk() != PK_TRANG && p.nj.getTypepk() != PK_DEN)) {
                gotoHaruna(p);
                return true;
            }
        } else if (this.map.isLdgtMap()) {
            if (p.getClanTerritoryData() == null) {
                gotoHaruna(p);
                return true;
            } else {
                p.getClanTerritoryData().getClanTerritory().setTime(p.nj);
            }
        }
        return false;
    }

    public void gotoHaruna(final @Nullable User p) throws IOException {
        if (p == null) {
            return;
        }

        p.nj.setMapid(27);
        val map = Server.getInstance().getMapById(27);
        p.nj.x = map.template.x0;
        p.nj.y = map.template.y0;
        try {
            Service.batDauTinhGio(p, 0);
            p.nj.getPlace().leave(p);
            p.nj.get().isDie = false;
            p.nj.get().upHP(p.nj.get().getMaxHP());
            p.nj.get().upMP(p.nj.get().getMaxMP());
        } catch (Exception e) {
        } finally {
            map.getFreeArea().Enter(p);
        }
    }

    public void changeMap(@Nullable final User p) throws IOException {
        if (p == null) {
            return;
        }
        val _ninja = p.nj;
        for (Mob mob : this._mobs) {
            if ((mob.templates.id == 230 || mob.templates.id == 221) && mob.tuanlocid == p.nj.get().id) {
                // this._mobs.remove(mob);
                break;
            }
        }
        if (TaskHandle.isLockChangeMap((short) map.id, p.nj.getTaskId())) {
            restPoint(_ninja);
            GameCanvas.startOKDlg(_ninja.p.session, Text.get(0, 84));
            return;
        }
        if (p.nj.addCuuSat || p.nj.isCuuSat) {
            p.removeCuuSat(p.nj);
        }
        this.leave(p);
        for (byte i = 0; i < this.map.template.vgo.length; ++i) {
            final Vgo vg = this.map.template.vgo[i];

            if (p.nj.get().x + 100 >= vg.minX && p.nj.get().x <= vg.maxX + 100 && p.nj.get().y + 100 >= vg.minY
                    && p.nj.get().y <= vg.maxY + 100) {
                int mapid;
                if (this.map.id == 138) {
                    mapid = (new int[]{134, 135, 136, 137})[util.nextInt(4)];
                } else {
                    mapid = vg.mapid;
                }

                Map ma = Manager.getMapid(mapid);
                if (mapid == 22 || mapid == 1 || mapid == 72 || mapid == 27) {
                    p.nj.changeTypePk((short) 0);
                }
                if (this.map.cave != null) {
                    for (byte j = 0; j < this.map.cave.map.length; ++j) {
                        if (this.map.cave.map[j].id == mapid) {
                            ma = this.map.cave.map[j];
                        }
                    }
                }
                for (byte j = 0; j < ma.template.vgo.length; ++j) {
                    final Vgo vg2 = ma.template.vgo[j];
                    if (vg2.mapid == this.map.id) {
                        p.nj.get().x = vg2.goX;
                        p.nj.get().y = vg2.goY;
                    }
                }
                byte errornext = -1;
                for (byte n = 0; n < p.nj.get().ItemMounts.length; ++n) {
                    if (p.nj.get().ItemMounts[n] != null && p.nj.get().ItemMounts[n].isExpires
                            && p.nj.get().ItemMounts[n].expires < System.currentTimeMillis()) {
                        errornext = 1;
                    }
                }
                if (map.isLdgtMap()) {
                    if (p.getClanTerritoryData() == null || p.getClanTerritoryData().getClanTerritory() == null) {
                        errornext = 5;
                    } else {
                        Place place = p.getClanTerritoryData().getClanTerritory().openedMap.get(ma.id);
                        if (place == null) {
                            if (map.id != 89) {
                                errornext = 6;
                            } else {
                                if (vg.minX == 20) {
                                    place = p.getClanTerritoryData().getClanTerritory().openedMap.get(84);
                                    if (place.canEnter()) {
                                        p.nj.enterSamePlace(place, null);
                                        return;
                                    } else {
                                        errornext = 7;
                                    }
                                } else {
                                    errornext = 6;
                                }

                            }
                        } else {
                            if (place.canEnter) {
                                changeToPlace(p, vg, mapid, place);
                                return;
                            } else {
                                errornext = 7;
                            }
                        }
                    }
                } else if (map.isGtcMap()) {
                    if (p.nj.getClanBattle() != null) {
                        final java.util.Map<Byte, Place> openedMaps = p.nj.getClanBattle().openedMaps;
                        final Place place = openedMaps.get((byte) mapid);

                        if (p.nj.getPhe() == PK_TRANG && mapid == BAO_DANH_GT_HAC) {
                            errornext = 3;
                        } else if (p.nj.getPhe() == PK_DEN && mapid == BAO_DANH_GT_BACH) {
                            errornext = 3;
                        } else if (place != null) {
                            // TODO REMOVE COMMENT
                            if (p.nj.getClanBattle().getState() == WAITING_STATE) {
                                errornext = 9;
                            } else {
                                changeToPlace(p, vg, mapid, place);
                                return;
                            }
                        } else {
                            errornext = 8;
                        }
                    }
                } else {
                    // INcave map_back
                    if (this.map.cave != null && (this.map.getXHD() < 9 || this.map.getXHD() == 10)
                            && this.map.cave.map.length > this.map.cave.level
                            && this.map.cave.map[this.map.cave.level].id < mapid) {
                        errornext = 2;
                    }
                    // Not in time global battle
                    if (battle != null) {
                        if ((p.nj.getPhe() == PK_TRANG && mapid == CAN_CU_DIA_HAC)
                                || (p.nj.getPhe() == PK_DEN && mapid == CAN_CU_DIA_BACH)) {
                            errornext = 3;
                        }

                        if (battle.getState() != START_STATE) {
                            errornext = 4;
                        }
                    } else if (candyBattle != null) {
                        if (p.nj.getTypepk() == PK_DEN && mapid == CandyBattle.KEO_TRANG_ID
                                || p.nj.getTypepk() == PK_TRANG && mapid == CandyBattle.KEO_DEN_ID) {
                            errornext = 3;
                        }
                    }

                    // Has party
                    if (errornext == -1) {
                        if (p.nj.party != null) {
                            for (byte k = 0; k < ma.area.length; ++k) {

                                if (ma.area[k].getArryListParty().contains(p.nj.party)) {
                                    if (this.map.id == 138) {
                                        leave(p);
                                        ma.area[k].EnterMap0(p.nj);
                                        return;
                                    } else {
                                        p.nj.setMapid(mapid);
                                        p.nj.x = vg.goX;
                                        p.nj.y = vg.goY;

                                        if (p.nj.clone != null) {
                                            p.nj.clone.x = p.nj.x;
                                            p.nj.clone.y = p.nj.y;
                                        }
                                        leave(p);
                                        ma.area[k].Enter(p);
                                        return;
                                    }
                                }
                            }
                        }

                        for (byte k = 0; k < ma.area.length; ++k) {
                            if (ma.area[k].getNumplayers() < ma.template.maxplayers) {
                                if (this.map.id == 138) {
                                    leave(p);
                                    ma.area[k].EnterMap0(p.nj);
                                } else {
                                    p.nj.setMapid(mapid);
                                    p.nj.x = vg.goX;
                                    p.nj.y = vg.goY;

                                    if (p.nj.clone != null) {
                                        p.nj.clone.x = p.nj.x;
                                        p.nj.clone.y = p.nj.y;
                                    }
                                    leave(p);
                                    ma.area[k].Enter(p);
                                }
                                return;
                            }
                            if (k == ma.area.length - 1) {
                                errornext = 0;
                            }
                        }
                    }
                }

                if (errornext != -1) {
                    this.Enter(p);
                }
                switch (errornext) {
                    case 0: {
                        p.session.sendMessageLog("Bản đồ quá tải.");
                        return;
                    }
                    case 1: {
                        p.session.sendMessageLog("Trang bị thú cưới đã hết hạn. Vui lòng tháo ra để di chuyển");
                        return;
                    }
                    case 2: {
                        p.session.sendMessageLog("Cửa " + ma.template.name + " vẫn chưa mở");
                        return;
                    }
                    case 3: {
                        p.session.sendMessageLog("Không phân sự miễn vào");
                        return;
                    }
                    case 4: {
                        p.session.sendMessageLog("Chiến trường chưa bắt đầu con không thể đi tiếp");
                        return;
                    }
                    case 5: {
                        p.session.sendMessageLog("Không thể đi tiếp");
                        return;
                    }
                    case 6: {
                        p.session.sendMessageLog(ma.template.name + " chưa được mở");
                        return;
                    }
                    case 7: {
                        p.session.sendMessageLog("Phải mở đủ các cửa mới có thể đi tiếp");
                        break;
                    }
                    case 8: {
                        p.session.sendMessageLog("Lỗi không xác định");
                        break;
                    }
                    case 9: {
                        p.session.sendMessageLog("Gia tộc chiến chưa bắt đầu không thể đi tiếp");
                        break;
                    }
                }
            }
        }
    }

    private void changeToPlace(final @Nullable User p, @Nullable final Vgo vg, int mapid, @Nullable final Place place)
            throws IOException {
        if (p == null || vg == null || place == null) {
            return;
        }

        p.nj.setMapid(mapid);
        p.nj.x = vg.goX;
        p.nj.y = vg.goY;
        place.Enter(p);
    }

    private short MOVE_LIMIT = 80;
    private short RESET_LIMIT = 90;

    public void moveMessage(@Nullable Ninja nj, short x, short y) throws IOException {

        if (nj == null) {
            return;
        }
        final User u = nj.getPlace().getNinja(nj.name).p;
        if (nj.get().getEffId(18) != null) {
            return;
        }
        if (y >= this.map.template.max_y || y <= 0) {
            nj.get().upDie();
            return;
        }
        final short xold = nj.get().x;
        final short yold = nj.get().y;
        nj.y = y;
        val dx = Math.abs(xold - x);

        if (dx > nj.speed() * 18 && dx > MOVE_LIMIT && nj.hasBattle()) {
            nj.x = (short) ((xold + x) / 2);
            restPoint(nj);
            return;
        } else {
            if (nj.isNhanban) {
                nj.clone.x = x;
                nj.clone.y = y;
            }
            this.move(nj.get().id, nj.get().x, nj.get().y);
            nj.x = x;
            if (this.map.id == 4) {
                if (nj.get().x < 1069) {
                    if (nj.get().y > 240) {
                        nj.y = 240;
                        nj.x += 965;
                        restPoint(nj);
                    }
                }
            }
            if (this.map.id == 162) {
                if (nj.get().x < 1500) {
                    if (nj.get().y > 528) {
                        nj.y = 192;
                        nj.x = 1592;
                        restPoint(nj);
                    }
                }
                if (nj.get().y > 528) {
                    if (u.luong > 0) {
                        u.upluongMessage(-1);
                    } else {
                        if (nj.xu > 1000) {
                            nj.upXuMessage(-1000);
                        } else {
                            this.DieReturn(u);
                            return;
                        }

                    }
                }
            }
            if (this.map.id == 163) {
                if (nj.get().x < 500) {
                    if (nj.get().y > 240) {
                        nj.y = 144;
                        nj.x = 1139;
                        restPoint(nj);
                    }
                }
                if (nj.get().y > 264) {
                    if (u.luong > 0) {
                        u.upluongMessage(-1);
                    } else {
                        if (nj.xu > 1000) {
                            nj.upXuMessage(-1000);
                        } else {
                            this.DieReturn(u);
                            return;
                        }

                    }
                }
            }
            if (this.map.id == 161) {
                if (nj.get().x < 1300) {
                    if (nj.get().y > 288) {
                        nj.y = 264;
                        nj.x = 1854;
                        restPoint(nj);
                    }
                }
                if (nj.get().y > 312) {
                    if (u.luong > 0) {
                        u.upluongMessage(-1);
                    } else {
                        if (nj.xu > 1000) {
                            nj.upXuMessage(-1000);
                        } else {
                            this.DieReturn(u);
                            return;
                        }

                    }
                }
            }
            if (this.map.id == 137) {
                if (nj.get().y > 432) {
                    nj.y = 350;
                    nj.x = 950;
                    restPoint(nj);
                }
            }
            if (this.map.id == 136) {
                if (nj.get().x > 467 && nj.get().x < 646) {
                    if (nj.get().y == 312) {
                        nj.y = 144;
                        nj.x = 500;
                        restPoint(nj);
                    }
                }
            }
            if (this.map.id == 135) {
                if (nj.get().x < 251) {
                    if (nj.get().y > 264) {
                        nj.y = 288;
                        nj.x = 1185;
                        restPoint(nj);
                    }
                }
                if (nj.get().x < 400) {
                    if (nj.get().y > 312) {
                        nj.y = 288;
                        nj.x = 1185;
                        restPoint(nj);
                    }
                }
            }

            // TODO Reset for battle
            if (nj.hasBattle()) {
                if (nj.y > 264 || (264 - nj.y) >= RESET_LIMIT) {
                    nj.y = 264;
                    restPoint(nj);
                }
            } else if (nj.isBattleViewer) {
                if (nj.y < 300 || nj.y > 336) {
                    nj.y = 336;
                    restPoint(nj);
                }
            }

            if (nj.clone != null && nj.clone.isIslive()
                    && (Math.abs(nj.x - nj.clone.x) > 80 || Math.abs(nj.y - nj.clone.y) > 30)) {
                nj.clone.move((short) util.nextInt(nj.x - 35, nj.x + 35), nj.y);
            }
        }
    }

    public void moveMessage1(@Nullable Ninja nj, short x, short y) throws IOException {
        if (nj == null) {
            return;
        }

        if (nj.get().getEffId(18) != null) {
            return;
        }
        if (y >= this.map.template.max_y || y <= 0) { // fix độn thổ
            nj.get().upDie();
            return;
        }
        final short xold = nj.get().x;
        final short yold = nj.get().y;
        nj.y = y;
        val dx = Math.abs(xold - x);

        // if (xold > Math.abs(this.map.template.pxw - 35) || xold < 35) {
        // nj.x = (short) Math.abs(this.map.template.pxw - 35);
        // this.DieReturn(nj.p);
        // return;
        // }
        if (dx > nj.speed() * 18 && dx > MOVE_LIMIT && nj.hasBattle()) {
            nj.x = (short) ((xold + x) / 2);
            restPoint(nj);
            return;
        } else {
            if (nj.isNhanban) {
                nj.clone.x = x;
                nj.clone.y = y;
            }
            this.move(nj.get().id, nj.get().x, nj.get().y);
            nj.x = x;
        }

        // TODO Reset for battle
        if (nj.hasBattle()) {
            if (nj.y > 264 || (264 - nj.y) >= RESET_LIMIT) {
                nj.y = 264;
                restPoint(nj);
            }
        } else if (nj.isBattleViewer) {
            if (nj.y < 300 || nj.y > 336) {
                nj.y = 336;
                restPoint(nj);
            }
        }

        if (nj.clone != null && nj.clone.isIslive()
                && (Math.abs(nj.x - nj.clone.x) > 80 || Math.abs(nj.y - nj.clone.y) > 30)) {
            nj.clone.move((short) util.nextInt(nj.x - 35, nj.x + 35), nj.y);
        }
    }

    protected static void restPoint(@Nullable Ninja ninja) {
        if (ninja == null) {
            return;
        }

        Message msg = null;
        try {
            msg = new Message((byte) 52);
            msg.writer().writeShort(ninja.x);
            msg.writer().writeShort(ninja.y);
            ninja.p.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void move(final int id, final short x, final short y) {
        try {
            final Message m = new Message(1);
            m.writer().writeInt(id);
            m.writer().writeShort(x);
            m.writer().writeShort(y);
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeItemMapMessage(final short itemmapid) throws IOException {
        final Message m = new Message(-15);
        m.writer().writeShort(itemmapid);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    @SneakyThrows
    public void pickItem(@Nullable final User p, @Nullable Message m) throws IOException {
        synchronized (this._itemMap) {
            if (p == null || m == null) {
                return;
            }

            if (m.reader().available() == 0) {
                return;
            }
            final short itemmapid = m.reader().readShort();
            m.cleanup();
            for (short i = 0; i < this._itemMap.size(); ++i) {
                if (this._itemMap.get(i).itemMapId == itemmapid) {
                    final ItemMap itemMap = this._itemMap.get(i);
                    if (!itemMap.visible) {
                        return;
                    }
                    final Item item = itemMap.item;
                    final ItemData data = ItemDataId(item.id);

                    if (itemMap.master != -1 && itemMap.master != p.nj.id) {
                        p.sendYellowMessage("Vật phẩm của người khác.");
                        return;
                    }
                    // if (Math.abs(itemMap.x - p.nj.get().x) > 50 || Math.abs(itemMap.y -
                    // p.nj.get().y) > 30) {
                    // p.sendYellowMessage("Khoảng cách quá xa.");
                    // return;
                    // }

                    val ninja = p.nj;

                    if (item.id == 903) {// vithu
                        if (!p.nj.hasItemInBag(855)) {
                            p.sendYellowMessage("Bạn cần có Dây Nenshi tronh hành trang để nhặt được Trứng Vĩ thú");
                            return;
                        }
                        p.nj.removeItemBags(855, 1);
                        boolean canPickItem = true;
                        val lastHp = ninja.hp;
                        Service.showWait("Nhặt Trứng vĩ thú", ninja);
                        Thread.sleep(10000L);
                        Service.endWait(ninja);
                        if (lastHp < ninja.hp) {
                            canPickItem = false;
                        }
                        if (!canPickItem) {
                            return;
                        }
                    }

                    if (data.type == 19 || p.nj.getAvailableBag() > 0
                            || (p.nj.getIndexBagid(item.id, item.isLock()) != -1 && data.isUpToUp)) {
                        boolean canPickItem = true;
                        boolean isTaskItem = TaskHandle.itemPick(ninja, item.getData().id);

                        if (isTaskItem) {
                            itemMap.item.setLock(true);
                            TaskTemplate task = null;
                            if (taskTemplates.length > ninja.getTaskId()) {
                                task = taskTemplates[ninja.getTaskId()];
                            }
                            boolean isShowWaiting = itemMap != null && task != null
                                    && itemMap.item.id == (task.getItemsPick() != null
                                    && task.getItemsPick().length > ninja.getTaskIndex()
                                    ? task.getItemsPick()[ninja.getTaskIndex()]
                                    : -5)
                                    && ninja.getTaskId() != 31
                                    && item.id != 236
                                    && item.id != 221
                                    && item.id != 238
                                    && item.id != 349
                                    && item.id != 350
                                    && ninja.getTaskId() != 14
                                    && ninja.getTaskIndex() != 2
                                    && ninja.getTaskId() != 18
                                    && ninja.getTaskId() != 22
                                    && ninja.getTaskId() != 23;
                            if (isShowWaiting) {
                                val lastHp = ninja.hp;
                                Service.showWait("Nhặt Vật phẩm", ninja);
                                Thread.sleep(2000L);
                                Service.endWait(ninja);
                                if (lastHp != ninja.getMaxHP()) {
                                    canPickItem = false;
                                }
                            }
                            if (!canPickItem) {
                                return;
                            }

                            if (ninja.getAvailableBag() > 0) {
                                ninja.upMainTask();
                                if (itemMap.item.id == 238) {
                                    if (util.percent(100, 50)) {
                                        p.sendYellowMessage("Bạn đã bị dơi lửa đốt");
                                        p.nj.get().upHP(-1000);
                                    }
                                    itemMap.item.id++;
                                }
                                removeItemMap(p, i, itemMap);
                                if (ninja.party != null) {
                                    short k;
                                    for (k = 0; k < this.getNumplayers(); k = (short) (k + 1)) {
                                        Ninja player = this.getUsers().get(k).nj;

                                        if (player != null && player.p != null && player.party != null
                                                && player.id != ninja.id
                                                && player.party.id == ninja.party.id
                                                && player.getTaskId() == ninja.getTaskId()
                                                && player.getTaskIndex() == ninja.getTaskIndex()
                                                && (player.getAvailableBag() != -1)) {
                                            player.upMainTask();
                                            val itemClone = item.clone();
                                            if (itemClone.id == 238) {
                                                itemClone.id++;
                                            }
                                            itemClone.setLock(true);
                                            player.addItemBag(item.getData().isUpToUp, itemClone);
                                        }
                                    }
                                }
                            }
                        } else {
                            removeItemMap(p, i, itemMap);
                        }

                        break;
                    } else {
                        p.session.sendMessageLog("Hành trang không đủ chỗ trống.");
                    }
                }
            }
        }
    }

    private void removeItemMap(final @Nullable User p, short index, final @Nullable ItemMap itemmap)
            throws IOException {
        if (p == null || itemmap == null) {
            return;
        }

        val item = itemmap.item;
        if (itemmap.removedelay != -1) {
            this._itemMap.remove(index);
        } else {
            this._itemMap.get(index).setVisible(false);
            this._itemMap.get(index).nextTimeRefresh = System.currentTimeMillis() + 2000L;
        }

        Message m;
        m = new Message(-13);
        m.writer().writeShort(itemmap.itemMapId);
        m.writer().writeInt(p.nj.get().id);
        m.writer().flush();
        this.sendMyMessage(p, m);
        m.cleanup();
        m = new Message(-14);
        m.writer().writeShort(itemmap.itemMapId);
        if (ItemDataId(item.id).type == 19) {
            p.nj.upyen(item.quantity);
            m.writer().writeShort(item.quantity);
        }
        m.writer().flush();
        p.sendMessage(m);
        m.cleanup();

        if (ItemDataId(item.id).type != 19) {
            if (itemmap.item.id == 238) {
                itemmap.item.id++;
            }
            p.nj.addItemBag(true, itemmap.item);
            return;
        }
    }

    /*
     * public void leaveItemBackground(@Nullable final User p, byte index) throws
     * IOException {
     * if (p == null) return;
     * synchronized (this._itemMap) {
     * Message m = null;
     * final Item itembag = p.nj.getIndexBag(index);
     * if (itembag == null || itembag.isLock()) {
     * return;
     * }
     * if (this._itemMap.size() > 100) {
     * this.removeItemMapMessage(this._itemMap.remove(0).itemMapId);
     * }
     * final short itemmapid = this.getItemMapNotId();
     * final ItemMap item = new ItemMap();
     * item.x = p.nj.get().x;
     * item.y = p.nj.get().y;
     * item.itemMapId = itemmapid;
     * item.item = itembag;
     * this._itemMap.add(item);
     * p.nj.ItemBag[index] = null;
     * m = new Message(-6);
     * m.writer().writeInt(p.nj.get().id);
     * m.writer().writeShort(item.itemMapId);
     * m.writer().writeShort(item.item.id);
     * m.writer().writeShort(item.x);
     * m.writer().writeShort(item.y);
     * m.writer().flush();
     * this.sendMyMessage(p, m);
     * m.cleanup();
     * m = new Message(-12);
     * m.writer().writeByte(index);
     * m.writer().writeShort(item.itemMapId);
     * m.writer().writeShort(item.x);
     * m.writer().writeShort(item.y);
     * m.writer().flush();
     * p.sendMessage(m);
     * m.cleanup();
     * }
     * }
     */
    public void leaveItemBackground(@Nullable final User p, byte index) throws IOException {
        if (p == null) {
            return;
        }
        synchronized (this._itemMap) {
            Message m = null;
            final Item itembag = p.nj.getIndexBag(index);
            if (itembag == null || itembag.isLock()) {
                return;
            }
            if (this._itemMap.size() > 100) {
                this.removeItemMapMessage(this._itemMap.remove(0).itemMapId);
            }
            final short itemmapid = this.getItemMapNotId();
            final ItemMap item = new ItemMap();
            item.x = p.nj.get().x;
            item.y = p.nj.get().y;
            item.itemMapId = itemmapid;
            item.item = itembag;
            p.nj.ItemBag[index] = null;
            m = new Message(-12);
            m.writer().writeByte(index);
            m.writer().writeShort(item.itemMapId);
            m.writer().writeShort(-1);
            m.writer().writeShort(-1);
            m.writer().flush();
            p.sendMessage(m);
            m.cleanup();
        }
    }

    private boolean killedTa = false;

    public void refreshMobs() {
        synchronized (this) {
            for (Mob mob : this.getMobs()) {
                this.refreshMob(mob.id, true);
            }
        }
    }

    public void refreshMob(final int mobid) {
        this.refreshMob(mobid, false);
    }

    public void refreshMob(final int mobid, boolean force) {
        try {
            synchronized (this) {
                if (!force) {
                    if (map.id == 78 || map.id == 74) {
                        return;
                    }
                }

                final Mob mob = this.getMob(mobid);
                if (mob == null) {
                    return;
                }

                mob.ClearFight();
                mob.sys = (byte) util.nextInt(1, 3);
                if (this.map.cave == null && mob.lvboss != 3 && !mob.isIsboss() && map.id != 74 && map.id != 78) {
                    if (mob.lvboss > 0) {
                        mob.lvboss = 0;
                    }
                    if (!this.map.isLdgtMap()) {
                        if (mob.level >= 10
                                && PERCENT_TA_TL > util.nextInt(100)
                                && this.numTA < 2 && this.numTL < 1 && candyBattle == null) {
                            mob.lvboss = util.nextInt(1, 2);
                        }
                    } else {
                        if (mob.templates.id != 81
                                && this.checkCleanMob(mob.templates.id)) {
                            mob.lvboss = 1;
                        }
                    }
                }

                if (this.map.cave != null && this.map.cave.finsh > 0 && this.map.getXHD() == 6) {
                    final int hpup = mob.templates.hp * (10 * this.map.cave.finsh + 100) / 100;
                    final int n = hpup;
                    mob.hpmax = n;
                    mob.hp = n;
                } else {

                    final int hp = mob.templates.hp;
                    mob.hpmax = hp;
                    mob.hp = hp;
                }
                if (mob.lvboss == 3) {
                    final int n2 = mob.hpmax * 200;
                    mob.hpmax = n2;
                    mob.hp = n2;
                } else if (mob.lvboss == 2) {
                    ++this.numTL;
                    final int n3 = mob.hpmax * 100;
                    mob.hpmax = n3;
                    mob.hp = n3;
                } else if (mob.lvboss == 1) {
                    ++this.numTA;
                    final int n4 = mob.hpmax * 10;
                    mob.hpmax = n4;
                    mob.hp = n4;
                }
                mob.status = 5;
                mob.isDie = false;
                mob.setTimeRefresh(0L);

                final Message m = new Message(-5);
                m.writer().writeByte(mob.id);
                m.writer().writeByte(mob.sys);
                m.writer().writeByte(mob.lvboss);
                m.writer().writeInt(mob.hpmax);
                m.writer().flush();
                this.sendMessage(m);
                m.cleanup();
            }
        } catch (IOException e) {

        }
    }

    private void attackMob(final int dame, final int mobid, final boolean fatal) throws IOException {
        final Message m = new Message(-1);
        m.writer().writeByte(mobid);
        final Mob mob = this.getMob(mobid);
        if (mob == null) {
            return;
        }
        m.writer().writeInt(mob.hp);
        m.writer().writeInt(dame);
        m.writer().writeBoolean(fatal);
        m.writer().writeByte(mob.lvboss);
        m.writer().writeInt(mob.hpmax);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    private void MobStartDie(final int dame, final int mobid, final boolean fatal) throws IOException {
        final Mob mob = this.getMob(mobid);
        if (mob == null) {
            return;
        }
        final Message m = new Message(-4);
        m.writer().writeByte(mobid);
        m.writer().writeInt(dame);
        m.writer().writeBoolean(fatal);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    @SneakyThrows
    public void sendXYPlayerWithEffect(@Nullable final User p, short lastX, short lastY) {
        if (p == null) {
            return;
        }
        val m = new Message(-137);
        m.writer().writeByte(-1);
        m.writer().writeInt(p.nj.get().id);
        m.writer().writeShort(lastX);
        m.writer().writeShort(lastY);
        sendMessage(m);
        m.cleanup();
    }

    public void sendXYPlayer(@Nullable final User p) throws IOException {
        if (p == null) {
            return;
        }
        final Message m = new Message(52);
        m.writer().writeShort(p.nj.get().x);
        m.writer().writeShort(p.nj.get().y);
        m.writer().flush();
        p.sendMessage(m);
        m.cleanup();
    }

    private void setXYPlayers(final short x, final short y, @Nullable final User p1, @Nullable final User p2)
            throws IOException {
        if (p1 == null || p2 == null) {
            return;
        }

        final Body value = p1.nj.get();
        p2.nj.get().x = x;
        value.x = x;
        final Body value2 = p1.nj.get();
        p2.nj.get().y = y;
        value2.y = y;
        final Message m = new Message(64);
        m.writer().writeInt(p1.nj.get().id);
        m.writer().writeShort(p1.nj.get().x);
        m.writer().writeShort(p1.nj.get().y);
        m.writer().writeInt(p2.nj.get().id);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    public void removeMessage(final int id) {
        try {
            final Message m = new Message(2);
            m.writer().writeInt(id);
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCharInfo(@Nullable final User p, @Nullable final User revc) {
        if (p == null || revc == null) {
            return;
        }

        try {

            Message m = new Message(3);
            m.writer().writeInt(p.nj.get().id);
            m.writer().writeUTF(p.nj.clan.clanName);
            if (!p.nj.clan.clanName.isEmpty()) {
                m.writer().writeByte(p.nj.clan.typeclan);
            }
            m.writer().writeBoolean(false);
            m.writer().writeByte(p.nj.get().getTypepk());
            m.writer().writeByte(p.nj.get().nclass);
            m.writer().writeByte(p.nj.gender);
            m.writer().writeShort(p.nj.get().partHead());
            m.writer().writeUTF(p.nj.name);
            m.writer().writeInt(p.nj.get().hp);
            m.writer().writeInt(p.nj.get().getMaxHP());
            m.writer().writeByte(p.nj.get().getLevel());
            m.writer().writeShort(p.nj.get().Weapon());
            m.writer().writeShort(p.nj.get().partBody());
            m.writer().writeShort(p.nj.get().partLeg());
            m.writer().writeByte(-1);
            m.writer().writeShort(p.nj.get().x);
            m.writer().writeShort(p.nj.get().y);
            m.writer().writeShort(p.nj.get().eff5buffHP());
            m.writer().writeShort(p.nj.get().eff5buffMP());
            m.writer().writeByte(0);
            m.writer().writeBoolean(p.nj.isHuman);
            m.writer().writeBoolean(p.nj.isNhanban);
            m.writer().writeShort(p.nj.get().partHead());
            m.writer().writeShort(p.nj.get().Weapon());
            m.writer().writeShort(p.nj.get().partBody());
            m.writer().writeShort(p.nj.get().partLeg());
            Item item0 = p.nj.get().ItemBody[18];// Đầu thân chân
            if (item0 != null) {
                if (item0.id == 795) {// Thiên Nguyệt Chi Nữ
                    m.writer().writeShort(37);
                    m.writer().writeShort(38);
                    m.writer().writeShort(39);
                } else if (item0.id == 796) {// Nhật Tử Lam Phong
                    m.writer().writeShort(40);
                    m.writer().writeShort(41);
                    m.writer().writeShort(42);
                } else if (item0.id == 804) {// Hajiro
                    m.writer().writeShort(58);
                    m.writer().writeShort(59);
                    m.writer().writeShort(60);
                } else if (item0.id == 805) {// Shiraiji
                    m.writer().writeShort(55);
                    m.writer().writeShort(56);
                    m.writer().writeShort(57);
                } else if (item0.id == 991) {// áo dài nam
                    m.writer().writeShort(171);
                    m.writer().writeShort(172);
                    m.writer().writeShort(173);
                } else if (item0.id == 992) {// áo dài nữ
                    m.writer().writeShort(174);
                    m.writer().writeShort(175);
                    m.writer().writeShort(176);
                } else if (item0.id == 999) {// áo dài nam
                    m.writer().writeShort(171);
                    m.writer().writeShort(172);
                    m.writer().writeShort(173);
                } else if (item0.id == 1000) {// áo dài nữ
                    m.writer().writeShort(174);
                    m.writer().writeShort(175);
                    m.writer().writeShort(176);
                } else if (item0.id == 830) {// Mặt nạ hổ
                    m.writer().writeShort(69 - p.nj.gender * 3);
                    m.writer().writeShort(70 - p.nj.gender * 3);
                    m.writer().writeShort(71 - p.nj.gender * 3);
                } else {
                    m.writer().writeShort(-1);
                    m.writer().writeShort(-1);
                    m.writer().writeShort(-1);
                }
            } else {
                m.writer().writeShort(-1);
                m.writer().writeShort(-1);
                m.writer().writeShort(-1);
            }
            Item item3 = p.nj.get().ItemBody[17];// Vũ khí
            if (item3 != null) {
                if (item3.id == 799) {// Gậy Mặt Trăng
                    m.writer().writeShort(44);
                } else if (item3.id == 800) {// Gậy Trái tim
                    m.writer().writeShort(46);
                } else if (item3.id == 993) {// Đoạt mệnh kiếm
                    m.writer().writeShort(161);
                } else if (item3.id == 994) {// Đoạt mệnh đao
                    m.writer().writeShort(159);
                } else if (item3.id == 995) {// Đoạt mệnh dao
                    m.writer().writeShort(163);
                } else if (item3.id == 996) {// Gậy Trái cung
                    m.writer().writeShort(160);
                } else if (item3.id == 997) {// Gậy Trái tiêu
                    m.writer().writeShort(164);
                } else if (item3.id == 998) {// Gậy Trái phiến
                    m.writer().writeShort(162);
                } else {
                    m.writer().writeShort(-1);
                }
            } else {
                m.writer().writeShort(-1);
            }
            Item item4 = p.nj.get().ItemBody[12];// Yoroi
            if (item4 != null) {
                if (item4.id == 797) {// Hakairo Yoroi
                    m.writer().writeShort(43);
                } else {
                    m.writer().writeShort(-1);
                }
            } else {
                m.writer().writeShort(-1);
            }
            m.writer().writeShort(-1);// name
            Item item6 = p.nj.get().ItemMounts[4];// Thú cưỡi
            if (item6 != null) {
                if (item6.id == 798) {// Lân Sư Vũ
                    m.writer().writeShort(36);
                } else if (item6.id == 801) {// Xích Tử Mã
                    m.writer().writeShort(47);
                } else if (item6.id == 802) {// Tà Linh Mã
                    m.writer().writeShort(48);
                } else if (item6.id == 803) {// Phong Thương Mã
                    m.writer().writeShort(49);
                } else if (item6.id == 827) {// Phượng Hoàng Băng
                    m.writer().writeShort(63);
                } else if (item6.id == 831) {
                    m.writer().writeShort(72);
                } else if (item6.id == 968) { // hoả kỳ lân
                    m.writer().writeShort(117);
                } else {
                    m.writer().writeShort(-1);
                }
            } else {
                m.writer().writeShort(-1);
            }
            m.writer().writeShort(-1);// rank
            Item item8 = p.nj.get().ItemBody[27];// mặt nạ
            if (item8 != null) {
                if (item8.id == 813) {// Mặt nạ Shin Ah
                    m.writer().writeShort(54);
                } else if (item8.id == 814) {// Mặt nạ Vô Diện
                    m.writer().writeShort(53);
                } else if (item8.id == 815) {// Mặt nạ Oni
                    m.writer().writeShort(52);
                } else if (item8.id == 816) {// Mặt nạ Kuma
                    m.writer().writeShort(51);
                } else if (item8.id == 817) {// Mặt nạ Inu
                    m.writer().writeShort(50);
                } else {
                    m.writer().writeShort(-1);
                }
            } else {
                m.writer().writeShort(-1);
            }
            Item item9 = p.nj.get().ItemBody[26];// bienhinh
            if (item9 != null) {
                if (item9.id == 825) {// Pet Bóng Ma
                    m.writer().writeShort(61);// 61
                } else if (item9.id == 826) {// Pet Yêu Tinh
                    m.writer().writeShort(62);// 62
                } else {
                    m.writer().writeShort(-1);
                }
            } else {
                m.writer().writeShort(-1);
            }
            for (int k = 16; k < 32; ++k) {// Trang bị 2
                final Item item = p.nj.get().ItemBody[k];
                if (item != null) {
                    m.writer().writeShort(item.id);
                    m.writer().writeByte(item.getUpgrade());
                    m.writer().writeByte(item.sys);
                } else {
                    m.writer().writeShort(-1);
                }
            }
            m.writer().flush();
            revc.sendMessage(m);
            m.cleanup();
            if (p.nj.get().mobMe != null) {
                m = new Message(-30);
                m.writer().writeByte(-68);
                m.writer().writeInt(p.nj.get().id);
                m.writer().writeByte(p.nj.get().mobMe.templates.id);
                m.writer().writeByte(p.nj.get().mobMe.isIsboss() ? 1 : 0);
                m.writer().flush();
                revc.sendMessage(m);
                m.cleanup();
            }
        } catch (Exception e) {
        }
    }

    public void selectUIZone(@Nullable final User p, @Nullable Message m) throws IOException {
        if (p == null || m == null) {
            return;
        }

        final byte zoneid = m.reader().readByte();
        final byte index = m.reader().readByte();
        m.cleanup();
        if (this.map.id == 160) {// vithu
            p.sendYellowMessage("Không thể đổi khu ở map này");
            p.endLoad(true);
            return;
        }
        if (zoneid == this.id) {
            return;
        }
        Item item = null;
        try {
            item = p.nj.ItemBag[index];
        } catch (Exception ex) {
        }
        boolean isalpha = false;
        for (byte i = 0; i < this.map.template.npc.length; ++i) {
            final Npc npc = this.map.template.npc[i];
            if (npc.id == 13 && Math.abs(npc.x - p.nj.get().x) < 50 && Math.abs(npc.y - p.nj.get().y) < 50) {
                isalpha = true;
                break;
            }
        }
        if (((item != null && (item.id == 35 || item.id == 37)) || isalpha) && zoneid >= 0
                && zoneid < this.map.area.length) {
            if (this.map.area[zoneid].getNumplayers() < this.map.template.maxplayers) {
                this.leave(p);
                this.map.area[zoneid].Enter(p);
                p.endLoad(true);
                if (item != null && item.id != 37) {
                    p.nj.removeItemBag(index);
                }
            } else {
                p.sendYellowMessage("Khu vực này đã đầy.");
                p.endLoad(true);
            }
        }
        m = new Message(57);
        m.writer().flush();
        p.sendMessage(m);
        m.cleanup();
    }

    public void openUIZone(@Nullable final User p) throws IOException {
        if (p == null) {
            return;
        }

        boolean isalpha = false;
        for (byte i = 0; i < this.map.template.npc.length; ++i) {
            final Npc npc = this.map.template.npc[i];
            if (npc.id == 13 && Math.abs(npc.x - p.nj.get().x) < 50 && Math.abs(npc.y - p.nj.get().y) < 50) {
                isalpha = true;
                break;
            }
        }
        if (p.nj.quantityItemyTotal(37) > 0 || p.nj.quantityItemyTotal(35) > 0 || isalpha) {
            final Message m = new Message(36);
            m.writer().writeByte(this.map.area.length);
            for (byte j = 0; j < this.map.area.length; ++j) {
                m.writer().writeByte(this.map.area[j].getNumplayers());
                m.writer().writeByte(this.map.area[j].getArryListParty().size());
            }
            m.writer().flush();
            p.sendMessage(m);
            m.cleanup();
        } else {
            p.nj.get().upDie();
        }
    }

    public void chatNPC(@Nullable final User p, final int idnpc, final @Nullable String chat) throws IOException {
        if (p == null || chat == null) {
            return;
        }
        final Message m = new Message(38);
        m.writer().writeShort(idnpc);
        m.writer().writeUTF(chat);
        m.writer().flush();
        p.sendMessage(m);
        m.cleanup();
    }

    @SneakyThrows
    public ItemMap LeaveItem(final int id, int x, int y) {
        return LeaveItem(id, x, y, 1);
    }

    public ItemMap LeaveItem(final int id, int njX, int njY, int quantity) throws IOException {

        int rand = 0;
        if (id == 457) {
            rand = util.nextInt(0, 2);
            if (rand == 2) {
                return null;
            }
        }

        if (this._itemMap.size() > 100) {
            this.removeItemMapMessage(this._itemMap.remove(0).itemMapId);
        }

        final ItemData data = ItemDataId(id);
        if (data == null) {
            return null;
        }
        Item item;
        if (data.type < 10) {
            if (data.type == 1) {
                item = itemDefault(id);
                item.sys = GameScr.SysClass(data.nclass);
            } else {
                final byte sys = (byte) util.nextInt(1, 3);
                item = itemDefault(id, sys);
            }
        } else {
            item = itemDefault(id);
        }
        if (item.isTypeNgocKham() || item.isTypeBody()) {
            for (Option option : item.option) {
                option.param = util.nextInt(option.param * 70 / 100, option.param);
            }
        }
        final ItemMap im = new ItemMap();
        im.itemMapId = this.getItemMapNotId();

        im.x = (short) util.nextInt(njX - 2, njX + 2);
        im.y = (short) njY;
        im.item = item;
        item.quantity = quantity;
        this._itemMap.add(im);
        final Message m = new Message(6);
        m.writer().writeShort(im.itemMapId);
        m.writer().writeShort(item.id);
        m.writer().writeShort(im.x);
        m.writer().writeShort(im.y);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
        return im;
    }

    public void FightMob(@Nullable final Body body, @Nullable final Message m) throws IOException {
        if (body == null || m == null) {
            return;
        }

        val p = body.c.p;

        if (body.getCSkill() == -1 && body.getSkills().size() > 0) {
            body.setCSkill(body.getSkills().get(0).id);
        }

        final Skill skill = body.getMyCSkillObject();
        if (skill == null) {
            return;
        }
        final int mobId = m.reader().readUnsignedByte();
        m.cleanup();
        final Mob mob = this.getMob(mobId);
        if (mob == null) {
            return;
        }
        if (body instanceof IGlobalBattler) {
            if (((IGlobalBattler) body).getPhe() == PK_TRANG) {
                if (mob.templates.id == idBachMobs[0] || mob.templates.id == idBachMobs[1]) {
                    util.Debug("Không đánh bạch giả");
                    return;
                }
            } else if (((IGlobalBattler) body).getPhe() == PK_TRANG) {
                if (mob.templates.id == idHacMobs[0] || mob.templates.id == idHacMobs[1]) {
                    util.Debug("Không đánh hắc giả");
                    return;
                }
            }
        }
        if (mob == null || mob.isDie) {
            return;
        }

        final Mob[] arMob = new Mob[10];
        arMob[0] = mob;
        if (body.ItemBody[1] == null) {
            p.sendYellowMessage("Vũ khí không thích hợp");
            util.Debug("Không vũ khí không thích hợp");
            return;
        }
        p.removeEffect(15);
        p.removeEffect(16);
        final SkillTemplates data = SkillData.Templates(skill.id, skill.point);
        p.getMp();
        if (body.mp < data.manaUse && !(body instanceof CloneChar)) {
            MessageSubCommand.sendMP((Ninja) body);
            return;
        }

        if (skill.coolDown > System.currentTimeMillis()) {
            return;
        }

        if (Math.abs(body.x - mob.x) > body.getCSkillTemplate().dx + 30
                || Math.abs(body.y - mob.y) > body.getCSkillTemplate().dy + 30) {
            return;
        }
        skill.coolDown = System.currentTimeMillis() + data.coolDown;
        body.c.mobAtk = mob.id;

        if (body.isHuman) {
            body.upMP(-data.manaUse);
            p.getMp();
        }

        if (skill.id == 42) {
            body.x = mob.x;
            body.y = mob.y;
            // this.sendXYPlayer(p);
            this.setXYPlayers(mob.x, mob.y, p, p);
        }
        if (skill.id == 40) {
            if (mob.lvboss < 1 && !mob.isIsboss()) {
                this.DisableMobMessage(p, mob.id, 0);
            }
        }
        if (skill.id == 24) {
            this.DontMoveMobMessage(p, mob.id, 0);
        }
        if (skill.id == 4) {
            p.nj.get().upHP(p.nj.getPramSkill(50));
        }

        synchronized (this) {
            final int size = m.reader().available();
            byte n = 1;
            for (int i = 0; i < size; ++i) {
                final Mob mob2 = this.getMob(m.reader().readUnsignedByte());
                if (!mob2.isDie) {
                    if (mob.id != mob2.id) {
                        if (data.maxFight <= n) {
                            break;
                        }
                        arMob[n] = mob2;
                        ++n;
                    }
                }
            }
            m.cleanup();
            for (int j = 0; j < this.getUsers().size(); ++j) {
                Service.PlayerAttack(this.getUsers().get(j), arMob, body);
            }
            long xpup = 0L;
            for (byte k = 0; k < arMob.length; ++k) {
                if (arMob[k] != null) {
                    if (arMob[k].isDie) {
                        continue;
                    }
                    val stQuai = body.getPramItem(ST_LEN_QUAI_ID);
                    attackAMob(body, arMob[k], util.nextInt(body.dameMin(), body.dameMax()) + stQuai);
                }
            }

            if (xpup > 0L) {
                if (this.map.cave != null) {
                    this.map.cave.updateXP(xpup/30);
                } else {
                    if (p.nj.isNhanban) {
                        xpup /= 4L;
                    }
                    p.updateExp(xpup, true);
                    xpup /= 40L; // 5L
                    if (body.party != null) {
                        for (int i2 = 0; i2 < this.getUsers().size(); ++i2) {
                            final User p2 = this.getUsers().get(i2);
                            if (p2.nj.id != p.nj.id && p2.nj.party == p.nj.party
                                    && Math.abs(p2.nj.getLevel() - p.nj.getLevel()) <= 10) {
                                p2.updateExp(xpup/100, true);
                            }
                        }
                    }
                }
            }
        }
    }

    public synchronized void PlayerAttack(@Nullable final Ninja _char, @Nullable Mob[] arrMob,
            @Nullable Ninja[] arrChar) {
        if (_char == null) {
            return;
        }

        val p = _char.c.p;
        if (_char.getCSkill() == -1 && _char.getSkills().size() > 0) {
            _char.setCSkill(_char.getSkills().get(0).id);
        }
        final Skill skill = _char.getSkill(_char.getCSkill());
        if (skill == null) {
            return;
        }

        final SkillTemplates temp = SkillData.Templates(skill.id, skill.point);

        if (arrMob != null && arrChar != null) {
            try {
                short i;
                for (i = 0; i < this.getUsers().size(); i = (short) (i + 1)) {
                    if (this.getUsers().get(i).nj != null && this.getUsers().get(i).session != null
                            && this.getUsers().get(i).nj.id != _char.id) {

                        Service.PlayerAttack(this.getUsers().get(i).nj, _char.id, (byte) skill.id, arrMob, arrChar);
                    }
                }
            } catch (Exception exception) {
            }
        } else if (arrMob != null) {
            try {
                short i;
                for (i = 0; i < this.getUsers().size(); i = (short) (i + 1)) {
                    if (this.getUsers().get(i).nj != null && this.getUsers().get(i).session != null
                            && (this.getUsers().get(i).nj.id != _char.id)) {
                        Service.PlayerAttack(this.getUsers().get(i).nj, _char.id, (byte) skill.id, arrMob);
                    }
                }
            } catch (Exception exception) {
            }

        } else if (arrChar != null) {
            try {
                short i;
                for (i = 0; i < this.getUsers().size(); i = (short) (i + 1)) {
                    if (this.getUsers().get(i) != null && this.getUsers().get(i).session != null
                            && this.getUsers().get(i).id != _char.id) {

                        Service.PlayerAttack(this.getUsers().get(i).nj, _char.id, skill.id, arrChar);
                    }
                }
            } catch (Exception exception) {
            }
        }

        if (arrChar != null) {
            byte i;
            for (i = 0; i < arrChar.length; i = (byte) (i + 1)) {
                val player = arrChar[i];
                if (player != null) {

                    int dame = _char.dameMax();
                    if (Math.abs(_char.x - player.x) > temp.dx + util.nextInt(40) + i * 30
                            || Math.abs(_char.y - player.y) > temp.dy + util.nextInt(40) + i * 10
                            || (Map.notCombat(this.map.id) && (_char.getTypepk() == 1 || _char.getTypepk() == 3
                            || player.getTypepk() == 1 || player.getTypepk() == 3))) {
                        dame = 0;

                    }
                    if (dame != 0) {
                        if (_char.getTypepk() == Constants.PK_DOSAT
                                || player.getTypepk() == Constants.PK_DOSAT
                                || (_char.getTypepk() == PK_PHE && player.getTypepk() == PK_PHE)
                                || (_char.solo != null && player.solo != null)
                                || _char.getTypepk() == PK_TRANG || _char.getTypepk() == PK_DEN
                                || _char.getTypepk() == PK_PHE3) {
                            AttackPlayer(_char, player);
                        }
                    }
                }
            }
        }

        if (arrMob != null) {
            byte i;
            for (i = 0; i < arrMob.length; i = (byte) (i + 1)) {
                Mob mob = arrMob[i];
                if (mob != null) {
                    int fantal = _char.Fatal();
                    if (fantal > 750) {
                        fantal = 750;
                    }
                    boolean flag = (util.nextInt(1000) < fantal);
                    int dame = _char.dameMax();
                    if (Math.abs(_char.x - mob.x) > temp.dx + util.nextInt(40) + i * 30
                            || Math.abs(_char.y - mob.y) > temp.dy + util.nextInt(40) + i * 10) {
                        dame = 0;
                    }
                    if (dame != 0) {
                        AttackMob(_char, mob, util.nextInt(dame * 9 / 10, dame), flag, (byte) 0);
                    }
                }
            }
        }
    }

    @SneakyThrows
    protected void AttackMob(@Nullable Ninja _char, @Nullable Mob mob, int dame, boolean flag, byte type) {
        if (_char == null || mob == null) {
            return;
        }
        attackAMob(_char.get(), mob, dame + _char.getPramItem(ST_LEN_QUAI_ID));

    }

    public byte getId() {
        return this.id;
    }

    public void attackAMob(@Nullable final Body body, @Nullable Mob curMob, int dame) throws IOException {
        if (body == null || curMob == null) {
            return;
        }
        long xpup = 0L;
        if (curMob.isDie) {
            return;
        }
        if (curMob.zoneBoss != body.c.getPlace().getId() && curMob.isIsboss()) {
            return;
        }
        val p = body.c.p;

        if (body.getEffId(Mob.THIEU_DOT_ID) != null) {
            curMob.isThieuDot = true;
            curMob.masterThieuDot = body;
            curMob.mapThieuDot = this.map.id;
            curMob.zoneThieuDot = this.id;
        }
        if (this.map.cave == null && curMob.isIsboss() && body.getLevel() - curMob.level > 20
                && curMob.templates.id != 237) {
            dame = 0;
        }
        // if (this.map.template.id == 171 || this.map.template.id == 172 ||
        // this.map.template.id == 173) {
        // if (body.isIce || body.isWind) {
        // dame = 1;
        // }
        // }
        int xU = p.nj.get().x;
        int yU = p.nj.get().y;
        int xM = curMob.x;
        int yM = curMob.y;
        int a1 = (int) Math.pow(xU - xM, 2);
        int a2 = (int) Math.pow(yU - yM, 2);
        if (Math.sqrt(a1 + a2) > 300) {
            dame = 1;
        }
        if (this.map.template.id == 171 || this.map.template.id == 172 || this.map.template.id == 173) {
            int a = util.nextInt(100);
            if (curMob.isIsboss()) {
                if (a <= 20) {
                    FireNinjaMessage(p.nj.get().id, util.nextInt(0, 3));
                    IceNinjaMessage(p.nj.get().id, util.nextInt(0, 3));
                    WindNinjaMessage(p.nj.get().id, util.nextInt(0, 3));
                }
                if (body.isIce || body.isWind) {
                    dame = 1;
                }
            }

        }
        final int oldhp = curMob.hp;
        if (map.id != 171 || map.id != 172 || map.id != 173) {
            if (curMob.templates.id != 138) {
                if (body.getPramItem(134) >= util.nextInt(1, 100)) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 1, curMob.id, (byte) 65, 1, 1);
                        dame += (curMob.hpmax / 100) * 50;
                    }
                }
                if (body.getPramItem(135) >= util.nextInt(1, 100)) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 1, curMob.id, (byte) 64, 1, 1);
                        dame += (curMob.hpmax / 100) * 50;
                    }
                }
            }
        }
        int fatal = body.Fatal();
        // if (fatal > 800) {
        // fatal = 800;
        // }
        final boolean isfatal = fatal > util.nextInt(1, 1500);
        if (isfatal) {
            dame *= 2;
            dame = dame * (100 + body.FantalDamePercent()) / 100;
            dame += body.FantalDame();
        }
        if (dame <= 0) {
            dame = 1;
        }
        if (curMob.isFire) {
            dame *= 2;
        }
        if (p.nj.isNhanban) {
            dame = dame * p.nj.clone.percendame / 100;
        }
        if (curMob.sys == 1) {
            dame += dame * body.getPramItem(54) / 200;
        } else if (curMob.sys == 2) {
            dame += dame * body.getPramItem(55) / 200;
        } else if (curMob.sys == 3) {
            dame += dame * body.getPramItem(56) / 200;
        }
        int xpnew = dame / 25 * body.getLevel();
        if (body.getEffType((byte) 18) != null) {
            xpnew *= body.getEffType((byte) 18).param;
        }
        if (curMob.lvboss == 1) {
            xpnew *= 2;
        } else if (curMob.lvboss == 2) {
            xpnew *= 3;
        } else if (curMob.lvboss == 3) {
            xpnew /= 2;
        }
        if (this.map.isLangCo()) {
            xpnew = xpnew * 115 / 100;
        } else if (this.map.VDMQ()) {
            xpnew = xpnew * 115 / 100;
        }

        if ((this.map.cave != null || (curMob.level > 1 && Math.abs(curMob.level - body.getLevel()) <= 10)
                || this.map.isLangCo()) && curMob.templates.id != 230 && curMob.templates.id != 221) {
            xpup += xpnew;
        }

        if (curMob.templates.id == 223) {
            dame /= 4;
        }

        if (curMob.isIsboss()) {
            dame /= 2;
        }

        if (map.isLdgtMap()) {

            if (map.id == 90) {

                if (p.nj.getEffId(23) == null) {
                    curMob.updateHP(0);
                    p.sendYellowMessage("Bạn không thể nhìn rõ boss đi kiếm bông hoa ăn để khai nhãn");
                    return;

                } else {
                    curMob.updateHP(-dame);
                }
            } else {
                if (curMob.templates.id == 77) {
                    if (util.nextInt(0, 3) == 1) {
                        curMob.updateHP(0);
                    } else {
                        curMob.updateHP(-dame);
                    }
                } else {
                    curMob.updateHP(-dame);
                }
            }

        } else if (curMob.templates.id == 225) {
            curMob.updateHP(-1);
            // } else if (curMob.tuanlocid != p.nj.get().id) {
            // dame = 0;
        } else {
            curMob.updateHP(-dame);
        }
        if (this.map.template.id == 171 || this.map.template.id == 172 || this.map.template.id == 173) {
            int a = util.nextInt(100);
            if (a <= 5) {
                FireNinjaMessage(p.nj.get().id, util.nextInt(0, 3));
                IceNinjaMessage(p.nj.get().id, util.nextInt(0, 3));
                WindNinjaMessage(p.nj.get().id, util.nextInt(0, 3));
            }
        }
        if (curMob.isDie) {
            if (battle != null) {
                battle.updateBattler(body.c, false, curMob);
            }

            if (candyBattle != null) {
                candyBattle.updateBattler(body.c, false, curMob);
            }
        }
        if (body instanceof Ninja) {
            Ninja _ninja = (Ninja) body;
            if (curMob != null && curMob.isDie && TaskHandle.isMobTask(_ninja, curMob)) {
                _ninja.upMainTask();
                if (_ninja.party != null) {
                    synchronized (_ninja.party.getNinjas()) {
                        for (Ninja player : _ninja.party.getNinjas()) {
                            if (player != null
                                    && player.p != null
                                    && player.party != null
                                    && player.id != _ninja.id
                                    && player.getTaskId() == _ninja.getTaskId()
                                    && player.getTaskIndex() == _ninja.getTaskIndex()) {
                                player.upMainTask();
                            }
                        }
                    }
                }
            }
        }

        if ((curMob.templates.id != 0 || p.nj.getTaskId() == 40) && curMob.lvboss != 3 && !curMob.isIsboss()) {
            val _ninja = p.nj;
            if ((util.nextInt(100) < 50 || curMob.templates.id == 69)
                    && TaskHandle.itemDrop(_ninja, curMob) != -1 && curMob.isDie) {
                final ItemMap item = LeaveItem(TaskHandle.itemDrop(_ninja, curMob), _ninja.x + util.nextInt(-100, 100),
                        _ninja.y);
                item.master = _ninja.id;
                item.item.setLock(true);
            }
        }

        if (curMob.isDie) {
            if (curMob.templates.id == 223) {
                Server.timeRefreshBossSkHe = System.currentTimeMillis() + 1200000L;
                curMob.setTimeRefresh(System.currentTimeMillis() + 1200000L);
            }
            if (p.nj.getTasks()[0] != null && curMob.templates.id == p.nj.getTasks()[0].getKillId()
                    || p.nj.getTasks()[1] != null && curMob.templates.id == p.nj.getTasks()[1].getKillId()) {
                if (curMob.lvboss == 3) {
                    // Ta thu
                    p.nj.updateTaskOrder(TaskOrder.NHIEM_VU_TA_THU, 1);
                }
                p.nj.updateTaskOrder(TaskOrder.NHIEM_VU_HANG_NGAY, 1);

            }
            if (curMob.templates.id == 0 && p.nj.taskDanhVong[0] == 6) {
                if (p.nj.isTaskDanhVong == 1 && p.nj.taskDanhVong[0] == 6 && p.nj.nhiemvuDV) {
                    p.nj.taskDanhVong[1]++;
                }
                if (p.nj.taskDanhVong[1] == p.nj.taskDanhVong[2]) {
                    p.sendYellowMessage("Bạn đã hoàn thành nhiệm vụ danh vọng.");
                }
            }
            if (curMob.templates.id == 57
                    && p.nj.getTaskId() == 36
                    && p.nj.getTaskIndex() == 1) {
                val item = ItemData.itemDefault(taskTemplates[36].getItemsPick()[p.nj.getTaskIndex()]);
                item.quantity = 1;
                item.setLock(true);

                p.nj.addItemBag(false, item.clone());
                p.nj.upMainTask();
                val m = new Message(-6);
                if (p != null && p.nj != null && p.nj.party != null) {
                    for (Ninja ninja : p.nj.party.getNinjas()) {
                        if (ninja != null && ninja.id != p.nj.id
                                && ninja.getTaskId() == p.nj.getTaskId()
                                && p.nj.getTaskIndex() == ninja.getTaskIndex()) {
                            p.nj.addItemBag(false, item.clone());
                            p.nj.upMainTask();
                        }
                    }
                }
            }
            if (this.map.id == 169) {
                if (util.nextInt(0, 100) < 10) {
                    p.upluongMessage(util.nextInt(10, 40));

                }
            }
            if (util.nextInt(0, 100) < 6) {
                Item itemup = ItemData.itemDefault(38);
                p.nj.addItemBag(itemup.getData().isUpToUp, itemup);
            }
            if (curMob.templates.id == 237 && p.nj.getCurrentMapId() == 160) {// vithu
                for (User player : this._users) {
                    int zoneid = 0;
                    if (player.nj.get().getLevel() >= 41 && player.nj.get().getLevel() <= 50) {
                        zoneid = 1;
                    } else if (player.nj.get().getLevel() <= 60) {
                        zoneid = 2;
                    } else if (player.nj.get().getLevel() <= 70) {
                        zoneid = 3;
                    } else if (player.nj.get().getLevel() <= 80) {
                        zoneid = 4;
                    } else if (player.nj.get().getLevel() <= 90) {
                        zoneid = 5;
                    } else if (player.nj.get().getLevel() <= 100) {
                        zoneid = 6;
                    } else if (player.nj.get().getLevel() <= 110) {
                        zoneid = 7;
                    } else if (player.nj.get().getLevel() <= 120) {
                        zoneid = 8;
                    } else if (player.nj.get().getLevel() <= 130) {
                        zoneid = 9;
                    }
                    if (this.map.area[zoneid].getNumplayers() < this.map.template.maxplayers) {
                        this.leave(player);
                        this.map.area[zoneid].Enter(player);
                        player.endLoad(true);
                        server.manager.sendTB(player, "Hướng dẫn",
                                "Để nhặt được trứng cần có dây Nenshi trong hành trang. Nhặt trứng sẽ mất 10s, mất dây Nenshi ngay lập tức và không thể di chuyển cũng như đánh người khác, khi bị đánh mất máu hoặc chết sẽ nhặt trứng thất bại. Các nhẫn giả lưu ý!!!");
                    } else {
                        player.sendYellowMessage("Khu vực này đã đầy.");
                        player.endLoad(true);
                    }
                    player.nj.changeTypePkNormal((short) 3);
                    for (int i = 1; i < 10; i++) {
                        for (int j = 0; j < util.nextInt(1, 4); j++) {
                            int[] x = new int[]{87, 171, 283, 415, 523, 473, 675, 783, 915, 1029};
                            int[] y = new int[]{336, 264, 312, 336, 336, 240, 240, 240, 240, 240};
                            int a = util.nextInt(x.length);
                            LeaveItem(this.map.area[i], 903, x[a], y[a], 1);
                        }
                    }
                }
            }
        }

        if (dame > 0) {
            curMob.Fight(p.session.id, dame);
        }
        if (!curMob.isFire) {
            if (body.percentFire2() >= util.nextInt(1, PERCENT_SKILL_MAX)) {
                this.FireMobMessage(curMob.id, 0);
            }
            if (body.percentFire4() >= util.nextInt(1, PERCENT_SKILL_MAX)) {
                this.FireMobMessage(curMob.id, 1);
            }
        }

        if (!curMob.isIce && body.percentIce1_5() >= util.nextInt(1, PERCENT_SKILL_MAX)) {
            this.IceMobMessage(curMob.id, 0);
        }

        if (!curMob.isIce && body.percentIce2_3() >= util.nextInt(1, PERCENT_SKILL_MAX)) {
            this.IceMobMessage(curMob.id, 1);
        }

        if (!curMob.isWind) {
            if (body.percentWind1() >= util.nextInt(1, PERCENT_SKILL_MAX)) {
                this.WindMobMessage(curMob.id, 0);
            }
            if (body.percentWind2() >= util.nextInt(1, PERCENT_SKILL_MAX)) {
                this.WindMobMessage(curMob.id, 1);
            }
        }
        if (curMob.isDie) {
            this.MobStartDie(oldhp - curMob.hp, curMob.id, isfatal);
        } else {

            this.attackMob(oldhp - curMob.hp, curMob.id, isfatal);
        }

        if (map.isLdgtMap() && curMob.isDie && curMob.templates.id != 81) {
            int xuGt = 1;
            if (curMob.lvboss == 1) {
                xuGt = 5;
            } else {
                xuGt = 1;
            }

            p.getClanTerritoryData().getClanTerritory().upPoint(xuGt);
        }
        Random r = new Random();
        if (map.isLangCo() && curMob.isDie) {
            int a = r.nextInt(100);
            if (a < 30) {
                //p.upluongMessage(1);
                // p.nj.upxuMessage(100);
            }

        }
        // Chien truong keo
        if (candyBattle != null) {

            if (curMob.templates.id == CandyBattle.GIO_KEO_DEN_ID
                    || curMob.templates.id == CandyBattle.GIO_KEO_TRANG_ID) {
                if (curMob.templates.id == CandyBattle.GIO_KEO_DEN_ID && curMob.attackCount.get() >= 10) {
                    candyBattle.upPoint(PK_DEN, -5);
                    LeaveItem(CandyBattle.KEO_NGOT_ID, p.nj.x + util.nextInt(0, 10), p.nj.y, 5);
                    curMob.attackCount.set(0);
                    refreshMob(curMob.id, true);
                } else if (curMob.templates.id == CandyBattle.GIO_KEO_TRANG_ID && curMob.attackCount.get() >= 10) {
                    candyBattle.upPoint(PK_TRANG, -5);
                    LeaveItem(CandyBattle.KEO_NGOT_ID, p.nj.x + util.nextInt(0, 10), p.nj.y, 5);
                    curMob.attackCount.set(0);
                    refreshMob(curMob.id, true);
                }
            } else {
                LeaveItem(CandyBattle.KEO_NGOT_ID, p.nj.x + util.nextInt(0, 10), p.nj.y, 2);
            }
        }

        if (curMob.isDie && curMob.level > 1) {
            ++this.numMobDie;
            if (this.map.cave != null) {
                this.map.cave.updatePoint(1);
            }
            final int master = curMob.sortNinjaFight();

            if (!this.map.isLdgtMap()) {
                if (curMob.lvboss == 1) {
                    --this.numTA;
                    if (Math.abs(body.getLevel() - curMob.level) <= 10) {
                        val yen = util.nextInt(5000, 50000);
                        body.c.upyenMessage(yen);
                        p.sendYellowMessage("Bạn nhận được " + yen + " yên");
                        if (p.nj.isTaskDanhVong == 1 && p.nj.taskDanhVong[0] == 8 && p.nj.nhiemvuDV) {
                            p.nj.taskDanhVong[1]++;
                            if (p.nj.taskDanhVong[1] == p.nj.taskDanhVong[2]) {
                                p.sendYellowMessage("Bạn đã hoàn thành nhiệm vụ danh vọng.");
                            }
                        }
                    }
                } else if (curMob.lvboss == 2) {
                    --this.numTL;
                    if (Math.abs(body.getLevel() - curMob.level) <= 10) {
                        val yen = util.nextInt(5000, 70000);
                        body.c.upyenMessage(yen);
                        p.sendYellowMessage("Bạn nhận được " + yen + " yên");
                        if (p.nj.isTaskDanhVong == 1 && p.nj.taskDanhVong[0] == 9 && p.nj.nhiemvuDV) {
                            p.nj.taskDanhVong[1]++;

                            if (p.nj.taskDanhVong[1] == p.nj.taskDanhVong[2]) {
                                p.sendYellowMessage("Bạn đã hoàn thành nhiệm vụ danh vọng.");
                            }
                        }
                    }
                }
            } else {
                if (curMob.lvboss == 1) {
                    ItemMap itemMap = LeaveItem((short) 231, p.nj.x, p.nj.y);
                }
            }
            leaveItemLogic(body, curMob, p, master);

            if (this.map.cave != null
                    && (this.map.getXHD() < 9 || (this.map.getXHD() == 10 && this.map.cave.level < 1))) {
                curMob.isRefresh = false;
                if (this.getMobs().size() == this.numMobDie) {
                    if (this.map.getXHD() == 5) {
                        if (this.map.id == 105) {
                            this.map.cave.openMap();
                            this.map.cave.openMap();
                            this.map.cave.openMap();
                        } else if (this.map.id == 106 || this.map.id == 107 || this.map.id == 108) {
                            final Cave cave2 = this.map.cave;
                            ++cave2.finsh;
                            if (this.map.cave.finsh >= 3) {
                                this.map.cave.openMap();
                            }
                        } else {
                            this.map.cave.openMap();
                        }
                    } else if (this.map.getXHD() == 6 && this.map.id == 116) {
                        if (this.map.cave.finsh == 0) {
                            this.map.cave.openMap();
                        } else {
                            final Cave cave3 = this.map.cave;
                            ++cave3.finsh;
                        }
                        this.numMobDie = 0;
                        for (short l2 = 0; l2 < this.getMobs().size(); ++l2) {
                            this.refreshMob(l2);
                        }
                    } else {
                        this.map.cave.openMap();
                    }
                }
            }
        }
        if (p.nj.getPramItem(100) > 0) {// % exp
            xpup += xpup / 100 * p.nj.getPramItem(100);
        }
        if (xpup > 0L) {
            xpup /= 2;
            if (this.map.cave != null) {
                this.map.cave.updateXP(xpup / 30);
            } else {

                if (p.nj.isNhanban) {
                    xpup /= 2L;
                }
                p.updateExp(xpup, true);
                xpup /= 5L;
                if (body.party != null) {
                    for (int i2 = 0; i2 < this.getUsers().size(); ++i2) {
                        final User p2 = this.getUsers().get(i2);
                        if (p2.nj.id != p.nj.id && p2.nj.party == p.nj.party
                                && Math.abs(p2.nj.getLevel() - p.nj.getLevel()) <= 10) {
                            p2.updateExp(xpup/100, true);
                        }
                    }
                }
            }
        }
    }

    private void leaveItemLogic(@Nullable Body body, @Nullable Mob curMob, @Nullable User p, int master)
            throws IOException {
        if (body == null) {
            return;
        }

        short[] arid = new short[0];
        if (util.nextInt(100) < 16) {// tỷ lệ rớt dưới 25%
            if (this.map.isLangCo()) {
                arid = LANG_CO_ITEM_IDS;
                // arid = new short[]{12, 12, 12, 12, 12, 12, 12, 22, 22, 17, 17, 17, 17, 12,
                // 12, 12, 659, 38, 454, 455, 456, 486, 487, 488, 489, 841, 842, 843};
            } else if (util.nextInt(100) < 50) {// tỷ lệ rớt dưới 25%
                if (this.map.id == 169) {
                    arid = new short[]{12, 12, 12, 656, 657, 658, 12, 12, 12, 12, 22, 656, 657, 658, 22, 17, 17, 656,
                        657, 658, 17, 17, 12, 12, 12, 656, 657, 658};
                }
            }
        } else if (this.map.VDMQ()) {
            arid = (body.getEffId(41) == null && body.getEffId(40) == null) ? EMPTY : VDMQ_ITEM_IDS;
        } else if (map.id == 171 || map.id == 172 || map.id == 173) {

            arid = MAP_SHINBA;
        } else if (this.map.id >= 167 && this.map.id <= 168) {
            if (util.nextInt(1, 100) < 12) {// tỉ lệ ra đồ
                arid = new short[]{12, 12, 12, 1, 2, 3, 4, 5, 12, 154, 155, 134, 135, 164, 165, 144, 145, 124, 125,
                    189, 184, 179, 174, 114, 94, 104, 99, 109, 119};// item quai bosss moi
            } else {
                arid = new short[]{-1};
            }

        } else {
            int curMobMaxLv = curMob.level - curMob.level % 10 + 10;
            if (curMobMaxLv > 100) {
                curMobMaxLv = 100;
            }
            if (util.nextInt(1, 100) < 12) {// tỉ lệ ra đồ
                switch (curMobMaxLv) {// Vật phẩm rớt quái
                    case 10:
                        arid = new short[]{12, 12, 12, 1, 2, 3, 4, 5, 12, 154, 155, 134, 135, 164, 165, 144, 145, 124,
                            125, 189, 184, 179, 174, 114, 94, 104, 99, 109, 119};// thêm đá yên vật phẩm trang bị
                        break;
                    case 20:
                        arid = new short[]{12, 12, 12, 1, 2, 3, 4, 5, 12, 156, 157, 136, 137, 166, 167, 146, 147, 126,
                            127, 190, 185, 180, 175, 115, 95, 105, 100, 110, 120};
                        break;
                    case 30:
                        arid = new short[]{12, 12, 12, 3, 4, 5, 12, 158, 159, 138, 139, 168, 169, 148, 149, 128, 129,
                            191, 186, 181, 176, 116, 96, 106, 101, 111, 121};
                        break;
                    case 40:
                        arid = new short[]{12, 12, 12, 4, 5, 12, 160, 161, 140, 141, 170, 171, 150, 151, 130, 131,
                            192, 187, 182, 177, 117, 97, 107, 102, 112, 122};
                        break;
                    case 50:
                        arid = new short[]{12, 12, 12, 1, 2, 3, 4, 5, 12, 162, 163, 142, 143, 172, 173, 152, 153, 132,
                            133, 193, 188, 183, 178, 118, 98, 108, 103, 113, 123};
                        break;
                    case 60:
                        arid = new short[]{12, 12, 12, 1, 2, 3, 4, 5, 12, 325, 326, 327, 328, 329, 330, 317, 318, 319,
                            320, 321, 322, 323, 324, 332, 331, 333, 334, 335, 336};
                        break;
                    case 70:
                        arid = new short[]{12, 12, 12, 5, 12, 201, 202, 197, 198, 194};
                        break;
                    case 80:
                        arid = new short[]{12, 12, 12, 5, 12, 201, 202, 197, 198, 194};
                        break;
                    case 90:
                        arid = new short[]{12, 12, 12, 5, 12, 201, 202, 197, 198, 194};
                        break;
                    case 100:
                    case 110:
                    case 120:
                    case 130:
                    case 140:
                        arid = new short[]{12, 12, 12, 5, 12, 201, 202, 197, 198, 194};
                        break;

                }
            } else {
                arid = new short[]{-1};
            }
        }

        if (Math.abs(curMob.level - body.getLevel()) <= 10 && util.nextInt(1, 100) <= 20) {
            arid = new short[]{38};
        }
        // if (body.getLevel() > 79 && util.nextInt(1,100) <= 50) {
        // arid = new short[]{12};
        // }
        if (this.map.VDMQ() && body.getLevel() >= 100 && util.nextInt(100) <= 15
                && (curMob.lvboss == 1 || curMob.lvboss == 2)) {
            arid = new short[]{545};
        }

        // if (curMob.isIsboss()) {
        // updateBossItemDrop(curMob);
        // short[] aridboss = new short[0];
        // if (curMob.level >= 90) {
        // aridboss = new short[]{558,559,560,561,562,563};
        // ItemMap im = this.LeaveItem(aridboss[util.nextInt(0,1)], curMob.x +
        // util.nextInt(-20,20), curMob.y);
        // if (im != null) {
        // im.item.quantity = 1;
        // im.master = master;
        // }
        // }
        // }
        if (this.map.VDMQ() && body.getLevel() >= 100 && util.nextInt(100) <= 15
                && (curMob.lvboss == 1 || curMob.lvboss == 2)) {
            arid = new short[]{545};
        }

        if (curMob.isIsboss()) {
            updateBossItemDrop(curMob);
            short[] aridboss = new short[0];// boss vdmq
            if (curMob.templates.id == 203 || curMob.templates.id == 201 || curMob.templates.id == 204) {
                aridboss = new short[]{552, 553, 554, 555, 556, 557, 558, 559, 560, 561, 562, 563};
                // } else if (curMob.templates.id == 90) {
                // aridboss = new short[]{552,553,554,555,556,557,558,559,560,561,562,563}; //
                // boss langco
            }
            if (aridboss.length > 0) {
                // ItemMap im = this.LeaveItem(aridboss[util.nextInt(0, aridboss.length - 1)],
                // curMob.x + util.nextInt(-20, 20), curMob.y);
                Item itemup = ItemData.itemDefault(aridboss[util.nextInt(0, aridboss.length - 1)]);
                p.nj.addItemBag(itemup.getData().isUpToUp, itemup);
                // if (im != null) {
                // im.item.quantity = 1;
                // im.master = master;
                // }
            }
        }

        if (curMob.isIsboss() && curMob.level >= 45 && curMob.level <= 65) {
            short[] aridboss = new short[0];
            aridboss = new short[]{94, 95, 96, 114, 115, 116, 104, 105, 106, 119, 120, 121, 99, 100, 101, 109, 110,
                111};
            if (aridboss.length > 0) {
                // ItemMap im = this.LeaveItem(aridboss[util.nextInt(0, aridboss.length - 1)],
                // curMob.x + util.nextInt(-20, 20), curMob.y);
                // if (im != null) {
                // im.item.quantity = 1;
                // im.item.upgradeNext((byte) util.nextInt(6, 10));
                // im.master = master;
                // }
                Item itemup = ItemData.itemDefault(aridboss[util.nextInt(0, aridboss.length - 1)]);
                p.nj.addItemBag(itemup.getData().isUpToUp, itemup);
            }

        }

        if (Math.abs(curMob.level - p.nj.getLevel()) <= 10) {
            if (curMob.isDie) {
                if (p.nj.isTaskDanhVong == 1 && p.nj.taskDanhVong[0] == 7 && p.nj.nhiemvuDV) {
                    p.nj.taskDanhVong[1]++;
                    if (p.nj.taskDanhVong[1] == p.nj.taskDanhVong[2]) {
                        p.sendYellowMessage("Bạn đã hoàn thành nhiệm vụ danh vọng.");
                    }
                }

            }
        }
        if (map.isLdgtMap()
                && curMob.templates.id == 81) {
            // Lam thach thao
            if (40 >= util.nextInt(1, 100)) {
                final ItemMap itemMap = LeaveItem((short) 261, p.nj.x, p.nj.y);
                itemMap.item.expires = util.TimeMinutes(30);
                itemMap.master = master;
            }

        } else {
            int randomIndex = arid.length == 0 ? 0 : util.nextInt(arid.length);
//            if (server.manager.EVENT != 0 && Math.abs(curMob.level - body.getLevel()) <= 10 && util.nextInt(100) < 30) {
//                val eventItems = EventItem.getEventDropItemIds();
//                final int index = util.nextInt(0, eventItems.length - 1);
//                if (eventItems[index] != -1 && eventItems[index] != 298 && eventItems[index] != 299
//                        && eventItems[index] != 300 && eventItems[index] != 301) {// bỏ đồ rớt sk
//                    // val itemMap = this.LeaveItem(eventItems[index], p.nj.x + util.nextInt(-40,
//                    // 40), p.nj.y);
//                    Item itemup = ItemData.itemDefault(eventItems[index]);
//                    p.nj.addItemBag(itemup.getData().isUpToUp, itemup);
//                    // if (itemMap != null) {
//                    // itemMap.item.isExpires = false;
//                    // itemMap.item.quantity = 1;
//                    // itemMap.master = master;
//                    // }
//                }
//            }
if (server.manager.EVENT != 0 && Math.abs(curMob.level - body.getLevel()) <= 10) {
//                val eventItems = EventItem.getEventDropItemIds();
                 int[] eventItems = new int[]{ 607, 608, 609, 610};
                final int index = util.nextInt(0, eventItems.length - 1);
                if (eventItems[index] != -1) {
                    if (util.nextInt(1, 100) < 25) {
                        int itemId = eventItems[index];
                        Item item = ItemData.itemDefault(itemId);
                        item.isExpires = false;
                        item.quantity = 1;
                        p.nj.addItemBag(item.getData().isUpToUp, item);
                    }
                }
            }

            if (randomIndex > 0 && arid[randomIndex] != -1
                    && (this.map.isLangCo() || map.id == 171 || map.id == 172 || map.id == 173
                    || Math.abs(curMob.level - body.getLevel()) <= 10)) {
                if (arid[randomIndex] == 12) {
                    int yen = 0;
                    if (this.map.isLangCo()) {
                        yen = 600000;
                    } else {
                        yen = util.nextInt(20000, 55000);
                    }
                    p.nj.upyenMessage(yen);
                    p.sendYellowMessage("Bạn nhận được " + yen + " yên");
                } else {

                    Item itemup = ItemData.itemDefault(arid[randomIndex]);
                    if (itemup.getData().type < 10 || itemup.id < 12) {
                        final ItemMap im = this.LeaveItem(arid[randomIndex], p.nj.x, p.nj.y);
                        if (im != null) {
                            int quantity = 1;
                            im.item.quantity = quantity;
                            im.master = master;
                        }
                    } else {
                        if (itemup.id == 455 || itemup.id == 456) {
                            itemup.isExpires = true;
                            itemup.expires = util.TimeDay(7);
                        } else if (itemup.id == 545) {
                            itemup.isExpires = true;
                            itemup.expires = util.TimeDay(3);
                        }
                        p.nj.addItemBag(itemup.getData().isUpToUp, itemup);
                    }

                }
            }
        }

        if (curMob.isIsboss()) {
            if (this.map.cave == null) {
                if (curMob.templates.id != 230 && curMob.templates.id != 221) {
                    Manager.chatKTG(body.c.name + " đã tiêu diệt " + curMob.templates.name);
                }
            }
            if (this.map.VDMQ()) {
                final ItemMap im = this.LeaveItem((short) 547, p.nj.x, p.nj.y);
                if (im != null) {
                    im.master = master;
                }
            }
            int soluongitem = N_ITEM_BOSS;
            if (curMob.templates.id == 225 || curMob.templates.id == 230 || curMob.templates.id == 231) {
                soluongitem = 30;
            }
            if (curMob.templates.id == 138 || curMob.templates.id == 218) {
                for (int j = 0; j < soluongitem + 5; j++) {
                    // curMob.sortNinjaFight();
                    if (body.c.id == p.nj.id || curMob.sortNinjaFight() == -1) { // ?????? code gì đấy e
                        val itemId = curMob.templates.arrIdItem[j];
                        val item = itemDefault(itemId);
                        if (curMob.sortNinjaFight() == p.nj.id) {
                            body.c.addItemBag(item.getData().isUpToUp, item);
                        }
                    } else {
                        val itemId = curMob.templates.arrIdItem[j];
                        ItemMap im = this.LeaveItem(itemId, p.nj.x, p.nj.y);
                        if (im != null) {
                            if (im.item.id == 12) {
                                im.item.quantity = util.nextInt(Manager.MIN_YEN_BOSS, Manager.MAX_YEN_BOSS);
                            }
                            im.master = master;
                        }
                    }

                }
            }

            if (curMob.templates.id == 230 || curMob.templates.id == 221) {// vật phẩm boss tên trộm tuần lộc
                short[] aridboss = new short[]{12, 1, 2, 3, 4, 5, 6, 7, 8, 9};
                p.nj.addItemBag(false, ItemData.itemDefault(util.nextInt(0, aridboss.length)));
            } else {
                int lenghtItem;
                if (this.map.isLangCo()) {
                    lenghtItem = 30;
                } else {
                    lenghtItem = N_ITEM_BOSS;
                }
                for (int i = 0; i < lenghtItem; i++) {
                    val itemId = curMob.templates.arrIdItem[util.nextInt(0, curMob.templates.arrIdItem.length - 1)];
                    ItemMap im = this.LeaveItem(itemId, p.nj.x, p.nj.y);
                    if (im != null) {
                        if (im.item.id == 12) {
                            im.item.quantity = util.nextInt(Manager.MIN_YEN_BOSS, Manager.MAX_YEN_BOSS);
                        }
                        im.master = master;
                    }
                }
                body.c.upyenMessage(50000000);
            }

            if (this.map.cave != null && this.map.getXHD() == 9 && util.nextInt(3) < 3) {
                this.map.cave.updatePoint(this.getMobs().size());
                for (short k2 = 0; k2 < this.getMobs().size(); ++k2) {
                    this.getMobs().get(k2).updateHP(-this.getMobs().get(k2).hpmax);
                    this.getMobs().get(k2).isRefresh = false;
                    for (short h = 0; h < this.getUsers().size(); ++h) {
                        Service.setHPMob(this.getUsers().get(h).nj, this.getMobs().get(k2).id, 0);
                    }
                }
                final Cave cave = this.map.cave;
                ++cave.level;
            }
            if (this.map.cave != null && this.map.getXHD() == 10 && (this.map.id == 168 && this.map.cave.level == 1)) {
                this.map.cave.updatePoint(this.getMobs().size());
                for (short k2 = 0; k2 < this.getMobs().size(); ++k2) {
                    this.getMobs().get(k2).updateHP(-this.getMobs().get(k2).hpmax);
                    this.getMobs().get(k2).isRefresh = false;
                    for (short h = 0; h < this.getUsers().size(); ++h) {
                        Service.setHPMob(this.getUsers().get(h).nj, this.getMobs().get(k2).id, 0);
                    }
                }
                final Cave cave = this.map.cave;
                ++cave.level;
            }
        }
    }

    @SneakyThrows
    private void AttackPlayer(@Nullable final Ninja body, @Nullable Ninja other) {
        if (body == null || other == null) {
            return;
        }
        final int oldhp = other.hp;
        skillEffect(body, other);
        body.damage(other);
        if (other.isDie) {
            if (battle != null) {
                battle.updateBattler(body.c, other.isHuman, other);
            }

        }
        if (other.isDie) {

            if (body.getTypepk() == PK_DOSAT) {
                body.updatePk(1);
            }
            final long num1 = Level.getMaxExp(other.getLevel());
            final long num2 = Level.getLevel(other.getLevel()).exps;
            if (other.pk > 0) {
                if (other.getExp() > num1) {
                    other.expdown = 0L;
                    final Ninja ninja1 = other;
                    ninja1.setExp(ninja1.getExp() - num2 * (5 + other.pk) / 100L);
                    if (other.getExp() < num1) {
                        other.setExp(num1);
                    }
                } else {
                    other.setExp(num1);
                    final Ninja ninja2 = other;
                    ninja2.expdown += num2 * (5 + other.pk) / 100L;
                    if (other.expdown > num2 * 50L / 100L) {
                        other.expdown = num2 * 50L / 100L;
                    }
                }
                other.updatePk(-1);
            }
            other.type = 14;
            this.sendDie(other);
        }
    }

    private void skillEffect(@Nullable final Body body, @Nullable final Ninja other) {
        if (body == null || other == null) {
            return;
        }

        if (!other.isFire) {
            if (body.percentFire2() >= util.nextInt(1, PERCENT_SKILL_MAX)) {
                this.FireNinjaMessage(other.id, 0);
            }
            if (body.percentFire4() >= util.nextInt(1, PERCENT_SKILL_MAX)) {
                this.FireNinjaMessage(other.id, 1);
            }
        }

        if (!other.isIce) {
            if (body.percentIce1_5() >= util.nextInt(1, PERCENT_SKILL_MAX)) {
                this.IceNinjaMessage(other.id, 0);
            }
        }

        if (!other.isIce) {
            if (body.percentIce2_3() >= util.nextInt(1, PERCENT_SKILL_MAX)) {
                this.IceNinjaMessage(other.id, 1);
            }
        }

        if (other.nclass == KUNAI && !body.isIce) {
            if (other.percentIceKunai() >= util.nextInt(1, PERCENT_SKILL_MAX)) {
                this.IceNinjaMessage(body.id, 2);
            }
        }

        if (!other.isWind) {
            if (body.percentWind1() >= util.nextInt(1, PERCENT_SKILL_MAX)) {
                this.WindNinjaMessage(other.id, 0);
            }
            if (body.percentWind2() >= util.nextInt(1, PERCENT_SKILL_MAX)) {
                this.WindNinjaMessage(other.id, 1);
            }
        }
    }

    // rớt vật phẩm boss
    private void updateBossItemDrop(@Nullable final Mob mob) {
        if (mob == null) {
            return;
        }
        if (mob.templates.id == 223) {
            mob.templates.arrIdItem = new short[]{1, 2, 3, 4, 5, 6, 7};// boss sự kiện
        } else if (mob.templates.id == 220) {
            mob.templates.arrIdItem = new short[]{1, 2, 3, 4, 5, 6, 7};// boss sự kiện
        } else if (map.isLangCo()) {
            mob.templates.arrIdItem = new short[]{5, 6, 7, 8, 9, 5, 6, 7, 8, 9, 5, 6, 7, 8, 9, 10, 12, 456, 454, 455,
                10, 10, 10, 10, 10, 10, 545, 547, 443, 485, 436, 437, 549, 550, 551, 656, 656, 656, 657, 657, 657,
                658, 658, 658, 656, 656, 656, 656, 657, 657, 657, 658, 658, 658, 656, 656, 656, 656, 657, 657, 657,
                658, 658, 658, 656};
        } else if (map.id == 171 || map.id == 172 || map.id == 173) {
            mob.templates.arrIdItem = getItemBossShinba((short) 40);
        } else if (mob.level == 45) {
            mob.templates.arrIdItem = new short[]{5, 6, 7, 8, 9, 10, 12, 545, 443, 485, 549, 550, 551, 436, 437};
        } else if (mob.level == 55) {
            mob.templates.arrIdItem = new short[]{5, 6, 7, 8, 9, 10, 12, 545, 443, 485, 549, 550, 551, 436, 437};
        } else if (mob.level == 60) {
            mob.templates.arrIdItem = new short[]{5, 6, 7, 8, 9, 10, 12, 545, 443, 485, 549, 550, 551, 436, 437};
        } else if (mob.level == 65) {
            mob.templates.arrIdItem = new short[]{5, 6, 7, 8, 9, 10, 12, 545, 443, 485, 549, 550, 551, 436, 437};
        } else if (mob.level == 75) {
            mob.templates.arrIdItem = new short[]{5, 6, 7, 8, 9, 10, 12, 545, 443, 485, 549, 550, 551, 436, 437};
        } else if (mob.level == 90) {
            mob.templates.arrIdItem = new short[]{5, 6, 7, 8, 9, 10, 12, 545, 443, 340, 632, 633, 634, 635, 636, 637,
                485, 549, 550, 551, 436, 437};
        } else if (mob.level == 100) {
            mob.templates.arrIdItem = new short[]{5, 6, 7, 8, 9, 10, 12, 545, 443, 340, 632, 633, 634, 635, 636, 637,
                485, 549, 550, 551, 436, 437};
        } else if (mob.level == 99) {
            mob.templates.arrIdItem = new short[]{5, 6, 7, 8, 9, 10, 12, 545, 443, 340, 632, 633, 634, 635, 636, 637,
                485, 549, 550, 551, 436, 437};
        } else if (mob.level == 110) {
            mob.templates.arrIdItem = new short[]{5, 6, 7, 8, 9, 10, 12, 545, 443, 340, 632, 633, 634, 635, 636, 637,
                485, 549, 550, 551, 436, 437};
        } else {
            mob.templates.arrIdItem = new short[]{5, 6, 7, 8, 9, 10, 12, 545, 443, 485, 549, 550, 551, 436, 437};
        }

    }

    private short[] getItemBossShinba(short num) {
        short[] result = new short[num];
        Random random = new Random();
        int[] sach10x = new int[random.nextInt(5)];
        int[] daMaThuat = new int[15];
        // int[] daMaThuatMS = new int[15];
        int[] sx = new int[1];
        int[] arrda = new int[5];
        int[] da = {11, 11};
        int[] sach = {558, 559, 560, 561, 562, 563, 558, 559, 560, 561, 562, 563, 558, 559, 560, 561, 562, 563, 558,
            559, 560, 561, 562, 563, 560, 561, 562, 563};
        int r = util.nextInt(1, 3);
        for (int i = 0; i < num; i++) {
            result[i] = (short) 11;
            // sach
            // rd da
            for (int j = 0; j < arrda.length; j++) {
                int expXS = random.nextInt(num);
                arrda[j] = expXS;
            }
            for (int a = 0; a < arrda.length; a++) {
                int b = random.nextInt(da.length);
                if (i == arrda[a] && i != 0 && i != 1) {
                    result[i] = (short) da[b];
                }
            }
            // rd da ma thuat
            for (int j = 0; j < daMaThuat.length; j++) {
                int ttts = random.nextInt(num);
                daMaThuat[j] = ttts;
            }
            for (int a = 0; a < daMaThuat.length; a++) {
                if (i == daMaThuat[a] && i != 0 && i != 1) {
                    result[i] = (short) Integer.parseInt("1045");
                }
            }
            for (int j = 0; j < daMaThuat.length; j++) {
                int ttts = random.nextInt(num);
                daMaThuat[j] = ttts;
            }
            for (int a = 0; a < daMaThuat.length; a++) {
                if (i == daMaThuat[a] && i != 0 && i != 1) {
                    result[i] = (short) Integer.parseInt("1037");
                }
            }
            // rd sx
            for (int j = 0; j < sx.length; j++) {
                int ttts = random.nextInt(num);
                sx[j] = ttts;
            }
            for (int a = 0; a < sx.length; a++) {
                if (i == sx[a] && i != 0 && i != 1) {
                    result[i] = (short) Integer.parseInt("524");
                }
            }
            if (i == 0) {
                result[i] = (short) Integer.parseInt("1047");
            }
            if (i == 2) {
                if (r == 1) {
                    result[i] = (short) Integer.parseInt("1047");
                }
            }
            if (i == 1) {
                if (r == 1) {
                    result[i] = (short) sach[random.nextInt(sach.length)];
                }
            }
        }
        return result;
    }

    protected boolean canAttackNinja(final @Nullable Body body, final @Nullable Ninja other) {
        if (body == null || other == null) {
            return false;
        }

        if (body.party != null && other.party != null && body.party.ninjas.contains(other)) {
            return false;
        }

        short myPk = body.getTypepk();
        val p = body.c.p;

        if (body.isNhanban) {
            myPk = body.c.getTypepk();
        }

        short otherPk = other.get().getTypepk();
        return (body.ItemBody[1] != null && other.get() != null
                && ((myPk == 1 & otherPk == 1)
                || myPk == 3
                || otherPk == 3
                || (myPk == PK_TRANG && otherPk == PK_DEN)
                || (myPk == PK_DEN && otherPk == PK_TRANG))
                || (p.nj.solo != null
                && other.solo != null
                && p.nj.solo == other.solo)
                || ((p.nj.addCuuSat && other.isCuuSat) || (p.nj.isCuuSat && other.addCuuSat)));
    }

    public void attackNinja(final @Nullable Body body, @Nullable Message m) throws IOException {
        if (body == null || m == null) {
            return;
        }

        val p = body.c.p;
        final int ninjaId = m.reader().readInt();
        m.cleanup();
        // TODO CHECK

        if (GameScr.mapNotPK(this.map.id)) {
            if (body.isHuman && body.solo == null) {
                return;
            }
        }

        synchronized (this) {
            final Ninja other = this.getNinja(ninjaId);

            if (other == null) {
                return;
            }

            if (canAttackNinja(body, other)) {
                if (body.getCSkill() == -1 && body.getSkills().size() > 0) {
                    body.setCSkill(body.getSkills().get(0).id);
                }
                final Skill skill = body.getMyCSkillObject();
                if (skill == null || other.get().isDie || other.get().getEffId(15) != null
                        || other.get().getEffId(16) != null) {
                    return;
                }
                final Ninja[] arNinja = new Ninja[10];
                arNinja[0] = other;
                p.removeEffect(15);
                p.removeEffect(16);
                final SkillTemplates temp = body.getCSkillTemplate();

                if (body.mp < temp.manaUse) {
                    MessageSubCommand.sendMP((Ninja) body);
                    return;
                }

                final int rang = Integer.max(temp.dx, temp.dy) + 30;

                if (skill.coolDown > System.currentTimeMillis() || Math.abs(body.x - other.get().x) > rang
                        || Math.abs(body.y - other.get().y) > rang) {
                    return;
                }

                if (body.isIce || body.isWind) {
                    util.Debug("Choáng hoặc đóng băng");
                    return;
                }

                body.upMP(-temp.manaUse);
                skill.coolDown = System.currentTimeMillis() + temp.coolDown;
                if (skill.id == 24) {
                    other.p.setEffect(18, 0, body.getPramSkill(55) * 1000, 0);
                    return;
                }
                if (skill.id == 42) {
                    this.setXYPlayers(other.get().x, other.get().y, p, other.p);
                    other.p.setEffect(18, 0, 5000, 0);
                }
                byte n = 1;
                try {
                    while (temp.maxFight > n) {
                        final int idn = m.reader().readInt();
                        final Ninja nj2 = this.getNinja(idn);
                        if (nj2 != null && !nj2.isDie && nj2.getEffId(15) == null && other.get().id != body.id
                                && nj2.id != body.id && Math.abs(other.get().x - nj2.x) <= temp.dx) {
                            if (Math.abs(other.get().y - nj2.y) > temp.dy) {
                                continue;
                            }
                            if (nj2.getTypepk() == 3 || body.getTypepk() == 3
                                    || (body.getTypepk() == 1 && nj2.getTypepk() == 1 || nj2.getTypepk() == PK_TRANG
                                    || nj2.getTypepk() == PK_DEN)) {
                                arNinja[n] = nj2;
                            }
                            ++n;
                        }
                    }
                } catch (IOException ex) {
                }
                m = new Message(61);
                m.writer().writeInt(body.id);
                m.writer().writeByte(skill.id);
                for (byte i = 0; i < arNinja.length; ++i) {
                    final Ninja nj3 = arNinja[i];
                    if (nj3 != null) {
                        m.writer().writeInt(nj3.id);
                    }
                }
                m.writer().flush();
                this.sendMyMessage(p, m, body.isNhanban);
                m.cleanup();
                for (byte i = 0; i < arNinja.length; ++i) {
                    final Ninja nj4 = arNinja[i];
                    if (nj4 != null) {

                        final int oldhp = nj4.hp;

                        skillEffect(body, other);
                        body.damage(other);

                        if (nj4.isDie) {
                            if (body.getTypepk() == PK_DOSAT) {
                                body.updatePk(1);
                            }
                            if (p.nj.addCuuSat) {
                                p.removeCuuSat(nj4);
                                p.nj.get().updatePk(2);
                                nj4.p.removeCuuSat(p.nj);
                                nj4.p.sendYellowMessage("Bạn bị " + p.nj.name + " đánh trọng thương.");
                            }

                            if (battle != null) {
                                battle.updateBattler(body.c, nj4.isHuman, nj4);
                            } else if (candyBattle != null) {
                                candyBattle.updateBattler(body.c, true, nj4);
                            }

                            final long num1 = Level.getMaxExp(nj4.getLevel());
                            final long num2 = Level.getLevel(nj4.getLevel()).exps;
                            if (nj4.pk > 0) {
                                if (nj4.getExp() > num1) {
                                    nj4.expdown = 0L;
                                    final Ninja ninja1 = nj4;
                                    ninja1.setExp(ninja1.getExp() - num2 * (5 + nj4.pk) / 100L);
                                    if (nj4.getExp() < num1) {
                                        nj4.setExp(num1);
                                    }
                                } else {
                                    nj4.setExp(num1);
                                    final Ninja ninja2 = nj4;
                                    ninja2.expdown += num2 * (5 + nj4.pk) / 100L;
                                    if (nj4.expdown > num2 * 50L / 100L) {
                                        nj4.expdown = num2 * 50L / 100L;
                                    }
                                }
                                nj4.updatePk(-1);
                            }
                            nj4.type = 14;
                            this.sendDie(nj4);
                        }
                    }
                }
            }
        }
    }

    public void wakeUpDieReturn(final @Nullable User p) throws IOException {
        if (p == null) {
            return;
        }
        if (!p.nj.isDie || this.map.isLangCo() || p.nj.getCurrentMapId() == 111 || p.nj.getCurrentMapId() == 110
                || p.nj.getCurrentMapId() == 171 || p.nj.getCurrentMapId() == 172 || p.nj.getCurrentMapId() == 173) {
            return;
        }
        if (p.luong < 1) {
            p.session.sendMessageLog("Bạn không có đủ 1 lượng!");
            return;
        }
        p.nj.get().isDie = false;
        p.luongMessage(-1L);
        p.nj.get().hp = p.nj.get().getMaxHP();
        p.nj.get().mp = p.nj.get().getMaxMP();
        p.liveFromDead();
    }

    public void sendDie(final @Nullable Ninja c) throws IOException {
        if (c == null) {
            return;
        }
        if (c.get().getExp() > Level.getMaxExp(c.get().getLevel())) {
            final Message m = new Message(-11);
            m.writer().writeByte(c.get().pk);
            m.writer().writeShort(c.get().x);
            m.writer().writeShort(c.get().y);
            m.writer().writeLong(c.get().getExp());
            m.writer().flush();
            c.p.sendMessage(m);
            m.cleanup();
        } else {
            c.get().setExp(Level.getMaxExp(c.get().getLevel()));
            final Message m = new Message(72);
            m.writer().writeByte(c.get().pk);
            m.writer().writeShort(c.get().x);
            m.writer().writeShort(c.get().y);
            m.writer().writeLong(c.get().expdown);
            m.writer().flush();
            c.p.sendMessage(m);
            m.cleanup();
        }
        final Message m = new Message(0);
        m.writer().writeInt(c.get().id);
        m.writer().writeByte(c.get().pk);
        m.writer().writeShort(c.get().x);
        m.writer().writeShort(c.get().y);
        m.writer().flush();
        this.sendMyMessage(c.p, m);
        m.cleanup();
    }

    public void DieReturn(final @Nullable User p) throws IOException {
        if (p == null) {
            return;
        }

        this.leave(p);
        p.nj.get().isDie = false;

        Map ma = null;
        Place openedArea = null;
        if (this.candyBattle != null) {
            if (p.nj.getTypepk() == PK_TRANG) {
                resetDieReturn(p, this.candyBattle.getOpenMaps().get(CandyBattle.KEO_DEN_ID));
            } else if (p.nj.getTypepk() == PK_DEN) {
                resetDieReturn(p, this.candyBattle.getOpenMaps().get(CandyBattle.KEO_TRANG_ID));
            }
            return;
        }
        if (this.battle != null && p.nj.getTypepk() != Constants.PK_NORMAL) {
            if (map.isGtcMap()) {
                openedArea = p.nj.getPhe() == PK_TRANG ? p.nj.getClanBattle().openedMaps.get(BAO_DANH_GT_BACH)
                        : p.nj.getClanBattle().openedMaps.get(BAO_DANH_GT_HAC);
            } else {
                ma = p.nj.getPhe() == PK_TRANG ? Manager.getMapid(CAN_CU_DIA_BACH) : Manager.getMapid(CAN_CU_DIA_HAC);
            }
        } else if (this.map.cave != null) {
            ma = this.map.cave.map[0];
        } else {
            ma = Manager.getMapid(p.nj.mapLTD);
        }

        if (map.isLdgtMap()
                && (p.getClanTerritoryData() != null && p.getClanTerritoryData().getClanTerritory() != null)) {
            val area = p.getClanTerritoryData().getClanTerritory().openedMap.get(80);
            if (area != null) {
                resetDieReturn(p, area);
                return;
            }
        }
        if (map.isGtcMap() && p.nj.getPhe() != PK_NORMAL) {
            resetDieReturn(p, openedArea);
            return;
        }

        if (ma != null) {
            for (final Place area : ma.area) {
                if (area.getNumplayers() < ma.template.maxplayers) {
                    resetDieReturn(p, area);
                    return;
                }
            }
        }
    }

    private void resetDieReturn(final @Nullable User p, final @Nullable Place area) throws IOException {
        if (p == null || area == null) {
            return;
        }

        area.EnterMap0(p.nj);
        p.nj.get().hp = p.nj.get().getMaxHP();
        p.nj.get().mp = p.nj.get().getMaxMP();
        Message m = new Message(-30);
        m.writer().writeByte(-123);
        m.writer().writeInt(p.nj.xu);
        m.writer().writeInt(p.nj.yen);
        m.writer().writeInt(p.luong);
        m.writer().writeInt(p.nj.get().getMaxHP());
        m.writer().writeInt(p.nj.get().getMaxMP());
        m.writer().writeByte(0);
        m.writer().flush();
        p.sendMessage(m);
        m.cleanup();
        m = new Message(57);
        m.writer().flush();
        p.sendMessage(m);
        m.cleanup();
    }

    public void attackNinja(final int dame, final int nid) throws IOException {
        final Ninja n = this.getNinja(nid);
        final Message m = new Message(62);
        m.writer().writeInt(nid);
        m.writer().writeInt(n.hp);
        m.writer().writeInt(dame);
        m.writer().writeInt(n.mp);
        m.writer().writeInt(0);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    @SneakyThrows
    public void sendFatalMessage(int dame, @Nullable final Ninja victim) {
        if (victim == null) {
            return;
        }

        val m = new Message(62);
        m.writer().writeInt(victim.id);
        m.writer().writeInt(victim.hp);
        m.writer().writeInt(-Math.abs(dame));
        m.writer().writeInt(victim.mp);
        m.writer().writeInt(0);
        m.writer().flush();
        sendMessage(m);
        m.cleanup();

    }

    private void DisableMobMessage(final User p, final int mobid, final int type) {
        try {
            final Mob mob = this.getMob(mobid);
            switch (type) {
                case -1: {
                    mob.isDisable = false;
                    break;
                }
                case 0: {
                    mob.isDisable = true;
                    mob.timeDisable = System.currentTimeMillis() + 1000 * p.nj.getPramSkill(48);
                    break;
                }
            }
            final Message m = new Message(85);
            m.writer().writeByte(mobid);
            m.writer().writeBoolean(mob.isDisable);
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void DontMoveMobMessage(final User p, final int mobid, final int type) {
        try {
            final Mob mob = this.getMob(mobid);
            switch (type) {
                case -1: {
                    mob.isDontMove = false;
                    break;
                }
                case 0: {
                    mob.isDontMove = true;
                    mob.timeDontMove = System.currentTimeMillis() + 1000 * p.nj.getPramSkill(55)
                            + p.nj.getPramSkill(62);
                    break;
                }
            }
            final Message m = new Message(86);
            m.writer().writeByte(mobid);
            m.writer().writeBoolean(mob.isDontMove);
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void FireMobMessage(final int mobid, final int type) {
        try {
            final Mob mob = this.getMob(mobid);
            if (mob == null) {
                return;
            }
            switch (type) {
                case -1: {
                    mob.isFire = false;
                    break;
                }
                case 0: {
                    mob.isFire = true;
                    mob.timeFire = System.currentTimeMillis() + 2000L;
                    break;
                }
                case 1: {
                    mob.isFire = true;
                    mob.timeFire = System.currentTimeMillis() + 4000L;
                    break;
                }
            }
            final Message m = new Message(89);
            m.writer().writeByte(mobid);
            m.writer().writeBoolean(mob.isFire);
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void IceMobMessage(final int mobid, final int type) {
        try {
            final Mob mob = this.getMob(mobid);
            if (mob == null) {
                return;
            }
            switch (type) {
                case -1: {
                    mob.isIce = false;
                    break;
                }
                case 0: {
                    mob.isIce = true;
                    mob.timeIce = System.currentTimeMillis() + 1500L;
                    break;
                }
                case 1: {
                    mob.isIce = true;
                    mob.timeIce = System.currentTimeMillis() + 3000L;
                    break;
                }

            }
            final Message m = new Message(90);
            m.writer().writeByte(mobid);
            m.writer().writeBoolean(mob.isIce);
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void WindMobMessage(final int mobid, final int type) {
        try {
            final Mob mob = this.getMob(mobid);
            if (mob == null) {
                return;
            }
            switch (type) {
                case -1: {
                    mob.isWind = false;
                    break;
                }
                case 0: {
                    mob.isWind = true;
                    mob.timeWind = System.currentTimeMillis() + 1000L;
                    break;
                }
                case 1: {
                    mob.isWind = true;
                    mob.timeWind = System.currentTimeMillis() + 2000L;
                    break;
                }
            }
            final Message m = new Message(91);
            m.writer().writeByte(mobid);
            m.writer().writeBoolean(mob.isWind);
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void FireNinjaMessage(final int ninjaId, final int type) {
        try {
            Ninja ninja = this.getNinja(ninjaId);
            if (ninja == null) {
                return;
            }

            long reduceTime = 0;
            try {
                reduceTime = ninja.get().getPramSkill(37) * 100 + ninja.get().getFireReduceTime();
            } catch (Exception e) {

            }
            long time = 0;
            switch (type) {
                case -1: {
                    ninja.isFire = false;
                    break;
                }
                case 0: {
                    time = 2000L - reduceTime;
                    break;
                }
                case 1: {
                    time = 4000 - reduceTime;
                    break;
                }
                case 2: {
                    time = 5000 - reduceTime;
                    break;
                }
            }

            if (time > 0) {
                ninja.isFire = true;
                ninja.timeFire = System.currentTimeMillis() + time;
                ninja.p.setEffect(5, 0, (int) time, 10);
                MessageSubCommand.sendEffectToOther(ninja, ninja.getEffId(5), this.getUsers(), -1, -1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void IceNinjaMessage(final int ninjaId, final int type) {
        try {

            Ninja ninja = this.getNinja(ninjaId);
            if (ninja == null) {
                return;
            }
            long reduceIceTime = 0;

            try {
                reduceIceTime = ninja.get().getPramSkill(38) * 100 + ninja.get().getIceReduceTime();
            } catch (Exception e) {

            }
            long time = 0;

            switch (type) {
                case -1: {
                    ninja.isIce = false;
                    break;
                }
                case 0: {
                    time = 2000L - reduceIceTime;
                    break;
                }
                case 1: {
                    time = 3000L - reduceIceTime;
                    break;
                }
                case 2: {
                    time = 2000 - reduceIceTime;
                    break;
                }
                case 3: {
                    time = 5000 - reduceIceTime;
                    break;
                }
            }

            if (time > 0) {
                ninja.isIce = true;
                ninja.timeIce = System.currentTimeMillis() + time;
                ninja.p.setEffect(6, 0, (int) time, 10);
                MessageSubCommand.sendEffectToOther(ninja, ninja.getEffId(6), this.getUsers(), -1, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void WindNinjaMessage(final int ninjaId, final int type) {
        try {
            Ninja ninja = this.getNinja(ninjaId);
            if (ninja == null) {
                return;
            }
            long reduceTime = 0;
            try {
                reduceTime = ninja.get().getPramSkill(39) * 100 + ninja.get().getWindReduceTime();
            } catch (Exception e) {

            }
            long time = 0;

            switch (type) {
                case -1: {
                    ninja.isWind = false;
                    break;
                }
                case 0: {
                    time = 1000L - reduceTime;

                    break;
                }
                case 1: {
                    time = 2000 - reduceTime;
                    break;
                }
                case 3: {
                    time = 5000 - reduceTime;
                    break;
                }
            }

            if (time > 0) {
                ninja.isWind = true;
                ninja.timeWind = System.currentTimeMillis() + time;
                ninja.p.setEffect(7, 0, (int) time, 10);
                MessageSubCommand.sendEffectToOther(ninja, ninja.getEffId(7), this.getUsers(), -1, -1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadMobAttached(final int mobid) {
        synchronized (this) {
            try {
                final Mob mob = this.getMob(mobid);
                if (mob == null) {
                    return;
                }
                if (mob.isIce || mob.isWind || mob.isDisable) {
                    return;
                }
                long tFight = System.currentTimeMillis() + 1500L;
                if (mob.isIsboss()) {
                    tFight = System.currentTimeMillis() + 500L;
                }
                mob.timeFight = tFight;
                for (short i = 0; i < this.getUsers().size(); ++i) {
                    final User user = this.getUsers().get(i);
                    if (!user.nj.get().isDie && user.nj.get().getEffId(15) == null) {
                        if (user.nj.get().getEffId(16) == null) {
                            if (((user.nj.getMapId() == 99 || user.nj.getMapId() == 120)
                                    && user.nj.battleData.getPhe() == 4)
                                    || ((user.nj.getMapId() == 103 || user.nj.getMapId() == 124)
                                    && user.nj.battleData.getPhe() == 5)) {
                                return;
                            }
                            short dx = 80;
                            short dy = 2;
                            if (mob.templates.type > 3) {
                                dy = 80;
                            }
                            if (mob.isIsboss()) {
                                dx = 110;
                            }
                            if (user.session != null && mob.isFight(user.session.id)) {
                                dx = 200;
                                dy = 160;
                            }
                            if (Math.abs(user.nj.get().x - mob.x) < dx && Math.abs(user.nj.get().y - mob.y) < dy) {
                                int dame = mob.level * mob.level / 6;
                                dame *= 2; // quái thường
                                if (this.map.cave != null && this.map.cave.finsh > 0 && this.map.getXHD() == 6) {
                                    final int dup = dame = dame * (10 * this.map.cave.finsh + 100) / 100;
                                }
                                if (this.map.id == 171 || this.map.id == 172 || this.map.id == 173) {
                                    int dameDown = (dame * 25) / 100;
                                    dame -= dameDown;
                                }
                                if (this.map.id == 169) {
                                    dame +=5000;
                                }
                                if (mob.lvboss == 1) {// tinh anh
                                    if (this.map.id == 171 || this.map.id == 172 || this.map.id == 173) {
                                        dame *= 7;
                                    } else {
                                        dame *= 4;
                                    }
                                } else if (mob.lvboss == 2) {// thủ lĩnh
                                    dame *= 3;
                                } else if (mob.lvboss == 3) { // boss
                                    dame *= 4;
                                }
                                if (this.map.id == 134 || this.map.id == 135 || this.map.id == 136
                                        || this.map.id == 137) {
                                    int dameSub = dame / 5;
                                    dame += dameSub;
                                }
                                if (mob.isIsboss()) {
                                    if (mob.templates.id == 223) {
                                        dame *= 2;
                                    }
                                    dame *= 7;
                                }
                                if (mob.sys == 1) {
                                    dame -= user.nj.get().ResFire();
                                } else if (mob.sys == 2) {
                                    dame -= user.nj.get().ResIce();
                                } else if (mob.sys == 3) {
                                    dame -= user.nj.get().ResWind();
                                }
                                dame -= user.nj.get().dameDown();
                                dame = util.nextInt(dame * 90 / 100, dame);
                                if (dame <= 0) {
                                    dame = 1;
                                }
                                int miss = user.nj.get().Miss();

                                if (miss > 7500) {
                                    miss = 7500;
                                }
                                if (miss > util.nextInt(10000)) {
                                    dame = 0;
                                }
                                int mpdown = 0;
                                if (user.nj.get().hp * 100 / user.nj.get().getMaxHP() > 10) {
                                    final Effect eff = user.nj.get().getEffId(10);
                                    if (eff != null) {
                                        final int mpold = user.nj.get().mp;
                                        user.nj.get().upMP(-(dame * eff.param / 100));
                                        dame -= (mpdown = mpold - user.nj.get().mp);
                                    }
                                }
                                dame = PERCENT_DAME_BOSS * dame / 100;

                                if (mob.templates.id == 72) {
                                    // Fire ninja
                                    if (!user.nj.get().isFire && util.nextInt(0, 3) == 0) {
                                        FireNinjaMessage(user.nj.get().id, 2);
                                    }
                                } else if (mob.templates.id == 79) {
                                    // ICE
                                    if (!user.nj.get().isIce && util.nextInt(0, 3) == 0) {
                                        IceNinjaMessage(user.nj.get().id, 3);
                                    }
                                } else if (mob.templates.id == 76) {
                                    // Reflect Dame
                                    if (util.nextInt(0, 3) == 0) {
                                        final int maxDame = user.nj.get().dameMax();
                                        user.nj.get().upHP(-util.nextInt(maxDame * 20 / 100, maxDame * 30 / 100));
                                        MessageSubCommand.sendHP(user.nj.get(), this.getUsers());
                                    }
                                } else if (mob.templates.id == 74) {
                                    // Wind
                                    if (!user.nj.get().isWind && util.nextInt(0, 3) == 0) {
                                        WindNinjaMessage(user.nj.get().id, 3);
                                    }
                                }

                                if (user.nj.get().isFire) {
                                    dame *= 2;
                                }

                                if (mob.templates.id == 220) {
                                    dame = user.nj.getMaxHP() / 20;
                                }

                                if (user.nj.eff136 <= System.currentTimeMillis()) {// eff136
                                    dame -= dame * user.nj.getPramItem(136);
                                }

                                if (user.nj.get().nclass == KUNAI) {
                                    BuNhin buNhin = this.buNhins.stream().filter(b -> user.nj.get().id == b.ninjaId)
                                            .findFirst().orElse(null);
                                    if (buNhin != null) {
                                        buNhin.upHP(-dame);
                                        this.MobAtkBuNhinMessage(mob.id, i, (short) (-1), (byte) (-1), (byte) (-1));
                                        break;
                                    } else {
                                        user.nj.get().upHP(-dame);
                                    }
                                } else {
                                    user.nj.get().upHP(-dame);
                                }

                                if (!mob.isIce && user.nj.get().nclass == KUNAI) {
                                    if (user.nj.get().percentIceKunai() >= util.nextInt(1, PERCENT_SKILL_MAX)) {
                                        IceMobMessage(mob.id, 0);
                                    }
                                }

                                this.MobAtkMessage(mob.id, user.nj, dame, mpdown, (short) (-1), (byte) (-1),
                                        (byte) (-1));
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean checkCleanMob(int mobId) {
        return this.getMobs().stream().parallel().filter(m -> m.templates.id == mobId)
                .allMatch(m -> m.isDie);
    }

    private void MobAtkMessage(final int mobid, final @Nullable Ninja n, final int dame, final int mpdown,
            final short idskill_atk, final byte typeatk, final byte typetool) throws IOException {

        if (n == null) {
            return;
        }
        Message m = new Message(-3);
        m.writer().writeByte(mobid);
        m.writer().writeInt(dame);
        m.writer().writeInt(mpdown);
        m.writer().writeShort(idskill_atk);
        m.writer().writeByte(typeatk);
        m.writer().writeByte(typetool);
        m.writer().flush();
        n.p.sendMessage(m);
        m.cleanup();
        m = new Message(-2);
        m.writer().writeByte(mobid);
        m.writer().writeInt(n.id);
        m.writer().writeInt(dame);
        m.writer().writeInt(mpdown);
        m.writer().writeShort(idskill_atk);
        m.writer().writeByte(typeatk);
        m.writer().writeByte(typetool);
        m.writer().flush();
        this.sendMyMessage(n.p, m);
        if (n.isDie && !this.map.isLangCo()) {
            this.sendDie(n);
        }
    }

    private void loadMobMeAtk(@Nullable final Ninja n) {
        if (n == null) {
            return;
        }
        n.mobMe.timeFight = System.currentTimeMillis() + 3000L;
        try {
            if (n.mobAtk != -1 && (n.mobMe.templates.id >= 211 && n.mobMe.templates.id <= 217
                    || n.mobMe.templates.id == 70 || n.mobMe.templates.id == 122 || n.mobMe.templates.id == 229)) {
                final Mob mob = this.getMob(n.mobAtk);
                if (mob == null) {
                    return;
                }
                if (!mob.isDie && Math.abs(n.x - mob.x) < 150 && Math.abs(n.y - mob.y) < 150) {
                    val body = n.get();
                    val item = body.ItemBody[10];
                    int dame = item == null ? 500 : item.findParamById(ClanThanThu.ST_QUAI_ID);
                    if (n.mobMe.templates.id == 70) {
                        dame = 1000;
                    }
                    if (mob.level >= 70) {
                        n.p.updateExp(dame, true);
                    }
                    this.MobMeAtkMessage(n, mob.id, dame, (short) 40, (byte) 1, (byte) 1, (byte) 0);
                    mob.updateHP(-dame);
                    this.attackMob(dame, mob.id, false);
                } else {
                    n.mobAtk = -1;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void MobMeAtkMessage(final @Nullable Ninja n, final int idatk, final int dame, final short idskill_atk,
            final byte typeatk, final byte typetool, final byte type) throws IOException {

        if (n == null) {
            return;
        }
        final Message m = new Message(87);
        m.writer().writeInt(n.id);
        m.writer().writeByte(idatk);
        m.writer().writeShort(idskill_atk);
        m.writer().writeByte(typeatk);
        m.writer().writeByte(typetool);
        m.writer().writeByte(type);
        if (type == 1) {
            m.writer().writeInt(idatk);
        }
        m.writer().flush();
        n.p.sendMessage(m);
        m.cleanup();
    }

    public void openFindParty(@Nullable final User p) {
        if (p == null) {
            return;
        }
        try {
            final ArrayList<Party> partys = (ArrayList<Party>) this.getArryListParty();
            final Message m = new Message(-30);
            m.writer().writeByte(-77);
            for (int i = 0; i < partys.size(); ++i) {
                final Ninja n = partys.get(i).getNinja(partys.get(i).master);
                m.writer().writeByte(n.nclass);
                m.writer().writeByte(n.getLevel());
                m.writer().writeUTF(n.name);
                m.writer().writeByte(partys.get(i).ninjas.size());
            }
            m.writer().flush();
            p.sendMessage(m);
            m.cleanup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void update() throws Exception {
        synchronized (this) {

            synchronized (this._itemMap) {
                for (ItemMap itemMap : _itemMap) {
                    if (itemMap == null) {
                        continue;
                    }
                    if (itemMap.visible == false
                            && System.currentTimeMillis() > itemMap.nextTimeRefresh
                            && itemMap.removedelay == -1) {
                        itemMap.setVisible(true);
                    }
                }
            }
            val users = this.getUsers();
            for (int i = 0; i < users.size(); ++i) {
                try {
                    val user = this.getUsers().get(i);
                    if (user == null) {
                        continue;
                    }
                    if (user.nj.get().isDie) {
                        if (user.nj.isNhanban) {
                            user.nj.clone.off();
                        }
                        user.exitNhanBan(false);
                        // util.Debug("cho quay ve");
                    }
                    if (user.nj.getMapId() == 22 || user.nj.getMapId() == 1 || user.nj.getMapId() == 72) {
                        user.nj.changeTypePk((short) 0);
                    }
                    updateUser(user);
                    final Ninja ninja = user.nj;
                    if (ninja.isFire && System.currentTimeMillis() >= ninja.timeFire) {
                        this.FireNinjaMessage(ninja.id, -1);
                    }
                    if (ninja.isIce && System.currentTimeMillis() >= ninja.timeIce) {
                        this.IceNinjaMessage(ninja.id, -1);
                    }
                    if (ninja.isWind && System.currentTimeMillis() >= ninja.timeWind) {
                        this.WindNinjaMessage(ninja.id, -1);
                    }
                } catch (Exception e) {

                }
            }
            for (BuNhin buNhin : buNhins) {
                if (buNhin.expired()) {
                    removeBuNhin(buNhin);
                }
            }

            updateExpiredItemMap();
            for (int i = 0; i < this.getMobs().size(); ++i) {
                final Mob mob = this.getMobs().get(i);
                if (mob == null) {
                    continue;
                }
                mob.update(this);

                if (!map.isLdgtMap()
                        && mob.getTimeRefresh() > 0L
                        && System.currentTimeMillis() >= mob.getTimeRefresh()
                        && mob.isRefresh) {
                    this.refreshMob(mob.id);
                }

                if (mob.isDisable && System.currentTimeMillis() >= mob.timeDisable) {
                    this.DisableMobMessage(null, mob.id, -1);
                }
                if (mob.isDontMove && System.currentTimeMillis() >= mob.timeDontMove) {
                    this.DontMoveMobMessage(null, mob.id, -1);
                }
                if (mob.isFire && System.currentTimeMillis() >= mob.timeFire) {
                    this.FireMobMessage(mob.id, -1);
                }
                if (mob.isIce && System.currentTimeMillis() >= mob.timeIce) {
                    this.IceMobMessage(mob.id, -1);
                }
                if (mob.isWind && System.currentTimeMillis() >= mob.timeWind) {
                    this.WindMobMessage(mob.id, -1);
                }
                if (!mob.isDie && mob.status != 0 && mob.level != 1 && System.currentTimeMillis() >= mob.timeFight) {
                    this.loadMobAttached(mob.id);
                }
            }

            if (this.map.cave != null && System.currentTimeMillis() > this.map.cave.time) {
                this.map.cave.rest();
            }
            if (this.map.cave != null && this.map.cave.level == this.map.cave.map.length) {
                this.map.cave.finsh();
            }

            if (this.map.isLdgtMap()) {
                if (!recoverTa) {
                    if (this.checkCleanMob(this.map.getMobLdgtId())) {
                        final List<Mob> ldgtMobs = this.getMobs().stream()
                                .filter(m -> m.templates.id == this.map.getMobLdgtId())
                                .collect(Collectors.toList());
                        if (map.id != 80 && map.id != 90) {
                            val randomMob = ldgtMobs.get(util.nextInt(ldgtMobs.size()));
                            this.refreshMob(randomMob.id);
                            recoverTa = true;
                        }

                        this.getMobs().stream().filter(m -> m.templates.id == 81)
                                .forEach(m -> this.refreshMob(m.id));
                    }
                }

                if (map.id == 90 && checkCleanMob(BOST_LDGT_ID) && this.getUsers().size() > 0
                        && this.getUsers().get(0).getClanTerritoryData() != null
                        && this.getUsers().get(0).getClanTerritoryData().getClanTerritory() != null
                        && this.getUsers().get(0).getClanTerritoryData().getClanTerritory()
                                .getState() != ClanTerritory.State.WIN) {
                    this.getUsers().get(0).getClanTerritoryData().getClanTerritory().setState(ClanTerritory.State.WIN);
                }

            }
            val currentTime = System.currentTimeMillis();
            updateMission(currentTime);
        }
    }

    private void updateMission(long currentTime) throws IOException {
        /*
         * if (currentTime > 0) {
         * for (User user : getUsers()) {
         * if (user == null || user.nj == null) continue;
         * if ("Lồng đèn".equals(user.nj.name)) {
         * val ninjaAI = user.nj;
         * if (ninjaAI == null) continue;
         * if (ninjaAI.lastTimeMove == -1 ||
         * currentTime - ninjaAI.lastTimeMove >=
         * util.nextInt(20 * TIME_CONTROL_MOVE / 100, TIME_CONTROL_MOVE)) {
         * final int masterId = ninjaAI.masterId;
         * final Ninja ninja = PlayerManager.getInstance().getNinja(masterId);
         * if (!ninjaAI.isDie) {
         * if (Math.abs(ninja.x - ninjaAI.x) > 10) {
         * ninjaAI.x = (short) (ninja.x + util.nextInt(-20, 20));
         * moveMessage(ninjaAI, ninjaAI.x, ninja.y);
         * }
         * } else {
         * if (ninja != null) {
         * ninja.p.
         * sendYellowMessage("Nhiệm vụ thất bại do Lồng đèn được hộ tống kiệt sức");
         * leave(ninjaAI.p);
         * }
         * }
         * }
         * }
         * }
         * 
         * } else
         */
        if (map.id == 33) {
            for (User user : getUsers()) {
                if (user == null || user.nj == null) {
                    continue;
                }
                if ("Jaian".equals(user.nj.name)) {
                    val ninjaAI = user.nj;
                    if (ninjaAI == null) {
                        continue;
                    }
                    if (ninjaAI.lastTimeMove == -1
                            || currentTime - ninjaAI.lastTimeMove >= util.nextInt(20 * TIME_CONTROL_MOVE / 100,
                                    TIME_CONTROL_MOVE)) {
                        final int masterId = Math.abs(ninjaAI.id);
                        if (!ninjaAI.isDie) {
                            if (ninjaAI.x > 0) {
                                ninjaAI.x -= 50;
                                if (ninjaAI.x <= 0) {
                                    ninjaAI.x = 10;
                                }
                                moveMessage(ninjaAI, ninjaAI.x, this.map.template.npc[0].y);
                            }

                            if (ninjaAI.x >= 10 && ninjaAI.x <= 100) {
                                // Finish task
                                final Ninja ninja = PlayerManager.getInstance().getNinja(masterId);
                                if (ninja != null) {
                                    ninja.upMainTask();
                                    ninja.p.sendYellowMessage("Hoàn thành nhiệm vụ");
                                    leave(ninjaAI.p);
                                }
                                break;
                            }

                            final Ninja masterNinja = getUsers().stream()
                                    .filter(u -> u != null && u.nj != null && u.nj.id == masterId)
                                    .map(p -> p.nj).findFirst().orElse(null);
                            if (masterNinja == null) {
                                // Khong tim thay ninja trong map
                                final Ninja nj = PlayerManager.getInstance().getNinja(masterId);
                                if (nj != null) {
                                    // Tim thay ninja map khac
                                    nj.p.sendYellowMessage("Nhiệm vụ thất bại do rời khỏi map của Jaian");
                                    sendMapInfo(nj.p, this);
                                }
                                leave(ninjaAI.p);
                            } else {
                                // Ninja di xa nhan vat ho tong
                                if (masterNinja != null && Math.abs(ninjaAI.x - masterNinja.x) >= 500) {
                                    masterNinja.p.sendYellowMessage("Đi quá xa trẻ lạc nhiệm vụ thất bại");
                                    leave(ninjaAI.p);
                                    sendMapInfo(masterNinja.p, this);
                                }
                            }

                        } else {
                            final Ninja ninja = PlayerManager.getInstance().getNinja(masterId);
                            if (ninja != null) {
                                ninja.p.sendYellowMessage("Nhiệm vụ thất bại do trẻ được hộ tống kiệt sức");
                                leave(ninjaAI.p);
                                sendMapInfo(ninja.p, this);
                            }
                        }
                    }
                }
            }

        } else if (map.id == 74) {
            if (getUsers().size() == 1) {
                val nj = getUsers().get(0);
                if (nj == null) {
                    return;
                }
                if (nj.expiredTime == -1 || currentTime >= nj.expiredTime) {
                    for (User user : getUsers()) {
                        gietHeoRungGoBack(user, "Nhiệm vụ thất bại do hết thời gian");
                    }
                } else if (_mobs.stream().allMatch(m -> m.isDie == true) && _itemMap.size() == 0) {
                    for (User user : getUsers()) {
                        gietHeoRungGoBack(user, "Hoàn thành nhiệm vụ");
                    }
                }
            } else if (getUsers().size() > 1) {
                for (User user : getUsers()) {

                    leave(user);
                }
            }
        } else if (map.id == 78) {
            if (getUsers().size() == 1) {
                val u = getUsers().get(0);
                if (u != null) {
                    if (u.expiredTime == -1 || currentTime >= u.expiredTime) {
                        val nj = u.nj;
                        nj.getPlace().gotoHaruna(u);
                        Service.batDauTinhGio(u, 0);
                        u.sendYellowMessage("Thời gian trong địa đạo đã hết");
                    } else if (_mobs.stream().allMatch(m -> m.isDie)) {
                        if (_itemMap.size() == 0 && !u.nj.hasItemInBag(232)) {
                            if (u.nj.getTaskId() < taskTemplates.length) {
                                val task = taskTemplates[u.nj.getTaskId()];
                                val index = util.nextInt(0, _mobs.size()) % _mobs.size();
                                final ItemMap itemMap = LeaveItem(task.getItemsPick()[u.nj.getTaskIndex()],
                                        _mobs.get(index).x, _mobs.get(index).y);
                                u.expiredTime = System.currentTimeMillis() + 50000;
                                u.sendYellowMessage("Bạn có 50 giây để tìm tấm địa đồ");
                                Service.batDauTinhGio(u, 10);
                            } else {
                                u.sendYellowMessage("Không làm rơi vật phẩm nhiệm vụ nv địa đạo");
                            }
                        }

                    }
                }
            } else if (getUsers().size() > 1) {
                for (User u : getUsers()) {
                    if (u != null) {
                        if (u.nj != null && u.nj.getPlace() != null) {
                            u.nj.getPlace().gotoHaruna(u);
                            Service.batDauTinhGio(u, 0);
                            u.sendYellowMessage("Thời gian trong địa đạo đã hết");
                        }
                    }
                }
            }
        }
    }

    public static final int BOST_LDGT_ID = 116;

    private boolean recoverTa = false;

    
    // <editor-fold desc="Update event loop">
    @SneakyThrows
    private void updateUser1(final @Nullable User p) throws IOException {
        if (p == null) {
            return;
        }
        if (p.nj.get().fullTL() >= 7) {
            if (System.currentTimeMillis() > p.nj.delayEffect) {
                if (p.nj.get().effectFlag) {
                    p.nj.delayEffect = System.currentTimeMillis() + 1000L;
                    byte tl = 0;
                    switch (GameScr.SysClass(p.nj.nclass)) {
                        case 1: {
                            tl = 9;
                            break;
                        }
                        case 2: {
                            tl = 9;
                            break;
                        }
                        case 3: {
                            tl = 9;
                            break;
                        }
                    }
                    if (p.nj.fullTL() == 9) {
                        tl += 2;
                    }
                    if (p.nj.fullTL() == 8) {
                        tl += 1;
                    }
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, tl, 1, 1);
                    }
                    if (p.nj.get().getNgocEff() != 0) {
                        p.nj.get().effectFlag = !p.nj.get().effectFlag;
                    }
                }
            }
        } else {
            p.nj.get().effectFlag = false;
        }
        if (System.currentTimeMillis() > p.nj.delayEffect) {
            if (p.nj.get().getNgocEff() != -4) {
                if (p.nj.get().fullTL() < 7) {
                    p.nj.get().effectFlag = false;
                }

                if (!p.nj.get().effectFlag) {
                    p.nj.delayEffect = System.currentTimeMillis() + 5000L;
                    int fullNgoc = p.nj.get().getNgocEff();
                    if (fullNgoc == -2) {
                        short iClass = 0;
                        switch (GameScr.SysClass(p.nj.nclass)) {
                            case 1: {
                                iClass = 9;
                                break;
                            }
                            case 2: {
                                iClass = 3;
                                break;
                            }
                            case 3: {
                                iClass = 6;
                                break;
                            }
                        }
                        for (int k = 0; k < this.getUsers().size(); ++k) {
                            GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                    (short) iClass, 1, 1);
                        }
                    }
                    if (fullNgoc == -1) {
                        short iClass = 0;
                        switch (GameScr.SysClass(p.nj.nclass)) {
                            case 1: {
                                iClass = 10;
                                break;
                            }
                            case 2: {
                                iClass = 4;
                                break;
                            }
                            case 3: {
                                iClass = 7;
                                break;
                            }
                        }
                        for (int k = 0; k < this.getUsers().size(); ++k) {
                            GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                    (short) iClass, 1, 1);
                        }
                    }
                    if (fullNgoc == 1) {
                        short iClass = 0;
                        switch (GameScr.SysClass(p.nj.nclass)) {
                            case 1: {
                                iClass = 11;
                                break;
                            }
                            case 2: {
                                iClass = 5;
                                break;
                            }
                            case 3: {
                                iClass = 8;
                                break;
                            }
                        }
                        for (int k = 0; k < this.getUsers().size(); ++k) {
                            GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                    (short) iClass, 1, 1);
                        }
                    }

                    // if (fullNgoc < 0) {
                    // fullNgoc += 3;
                    // for (int k = 0; k < this.getUsers().size(); ++k) {
                    // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                    // (short) fullNgoc, 2000, 1);
                    // }
                    // } else if (fullNgoc <= 3) {
                    // if (fullNgoc > 2) {
                    // fullNgoc = 2;
                    // }
                    // short iClass = 0;
                    // switch (GameScr.SysClass(p.nj.nclass)) {
                    // case 1: {
                    // iClass = 9;
                    // break;
                    // }
                    // case 2: {
                    // iClass = 3;
                    // break;
                    // }
                    // case 3: {
                    // iClass = 6;
                    // break;
                    // }
                    // }
                    // for (int k = 0; k < this.getUsers().size(); ++k) {
                    // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                    // (short) (fullNgoc + iClass), 1, 1);
                    // }
                    // } else {
                    // }
                    if (p.nj.get().fullTL() >= 7) {
                        p.nj.get().effectFlag = !p.nj.get().effectFlag;
                    }
                }
            } else {
                p.nj.get().effectFlag = true;
            }
        }
        if (p.nj.get().getNgocEff() == 3) {
            short iClass = 0;
            switch (GameScr.SysClass(p.nj.nclass)) {
                case 1: {
                    iClass = 204;
                    break;
                }
                case 2: {
                    iClass = 206;
                    break;
                }
                case 3: {
                    iClass = 208;
                    break;
                }
            }
            for (int k = 0; k < this.getUsers().size(); ++k) {

                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) (iClass), 10000,
                        -1);
            }
        }
        synchronized (p) {
            removeIfItemExpired(p);
        }
        if (this.map.id == 160 || this.map.id == 162) {// vithu
            final Calendar rightNow = Calendar.getInstance();
            short moment = (short) rightNow.get(Manager.BOSS_WAIT_TIME_UNIT);
            if (this.id == 0) {
                p.nj.setTypepk((short) 0);
                p.nj.changeTypePkNormal((short) 0);
            }
            if (moment != 21) {
                this.DieReturn(p);
            }
        }
        if ((this.map.isLangCo() || this.map.id == 171 || this.map.id == 172 || this.map.id == 173)
                && (p.nj.isDie || p.nj.pk > 0L)) {
            this.DieReturn(p);
        }
        if (this.map.id == 169 && (p.nj.isDie || p.nj.pk > 0L || p.nj.get().getEffId(43) == null)) {// ||
            // !p.isSVip
            this.DieReturn(p);
        }
        try {
            for (byte i = 0; i < p.nj.ItemBag.length; i++) {
                Item item = p.nj.ItemBag[i];
                if (item != null && item.quantity < 0) {
                    item.setLock(true);
                    item.quantity = 1;
                }
            }

            // xoa item tui het han cua ng choi
            for (byte j = 0; j < p.nj.ItemBag.length; j++) {
                Item item = p.nj.ItemBag[j];
                if (item == null || !item.isExpires) {
                    continue;
                }
                if (System.currentTimeMillis() >= item.expires) {
                    p.nj.removeItemBag(j, item.quantity);
                }
            }
            // xoa item tra bi het han cua ng choi
            for (byte j = 0; j < p.nj.get().ItemBody.length; j++) {
                Item item = p.nj.get().ItemBody[j];
                if (item == null || !item.isExpires) {
                    continue;
                }
                if (System.currentTimeMillis() >= item.expires) {
                    if (p.nj.isHuman) {
                        p.nj.removeItemBody(j);
                    } else if (p.nj.isNhanban) {
                        p.nj.clone.removeItemBody(j);
                    }
                }
            }
            // xoa item ruong het han cua ng choi
            for (byte j = 0; j < p.nj.ItemBox.length; j++) {
                Item item = p.nj.ItemBox[j];
                if (item == null || !item.isExpires) {
                    continue;
                }
                if (System.currentTimeMillis() >= item.expires) {
                    p.nj.removeItemBox(j);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // if((this.map.isLangCo() || map.id == 75 || map.id == 76 || map.id == 77 ||
        // map.id == 79) && (p.nj.isDie || p.nj.pk > 0)) {
        // p.nj.place.leave(p);
        // Map ma = Manager.getMapid(p.nj.mapLTD);
        // byte k;
        // for (k = 0; k < ma.area.length; k++) {
        // if (ma.area[k].getNumplayers() < ma.template.maxplayers) {
        // ma.area[k].EnterMap0(p.nj);
        // return;
        // }
        // }
        // }
        updateEffect(p);
        try {
            if (System.currentTimeMillis() > p.nj.delayEffect2) {
                if (p.nj.get().getNgocEff() != 0) {
                    p.nj.delayEffect2 = System.currentTimeMillis() + 5000L;
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        if (p.nj.get().getNgocEff() >= 100) {
                            GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 5, 1,
                                    1);
                            GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 11, 1,
                                    1);
                        } else {
                            GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                    (short) p.nj.get().getNgocEff(), 1, 1);
                        }
                    }
                }
            }

            // if (p.nj.time136 >= System.currentTimeMillis()) {
            // for (int k = 0; k < this.getUsers().size(); ++k) {
            // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
            // (short)73, 1, 1);
            // }
            // }
            // if (p.nj.name.equals("admin")) {
            // for (int k = 0; k < this.getUsers().size(); ++k) {
            // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
            // (short)99, 1, 500);
            // }
            // }
            // if (p.nj.name.equals("admin")) {
            // for (int k = 0; k < this.getUsers().size(); ++k) {
            // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
            // (short)98, 1, 500);
            // }
            // }
            // if (p.nj.name.equals("admin") || p.nj.name.equals("") || p.nj.name.equals("")
            // || p.nj.name.equals("") || p.nj.name.equals("") || p.nj.name.equals("") ||
            // p.nj.name.equals("")) {
            // for (int k = 0; k < getUsers().size(); k++) {
            // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
            // (short)159, 1, 500);
            //// GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0,
            // p.nj.get().id, (short) 120, 1, 500);
            //// GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0,
            // p.nj.get().id, (byte) 120, 500, 1);
            //// GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0,
            // p.nj.get().id, (short) 5, 21000000, 10);
            //// GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0,
            // p.nj.get().id, (short) 8, 21050000, 5);
            //// GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0,
            // p.nj.get().id, (short) 11, 20050000, 3);
            // }
            // }
            if (p.tongnap > 1000000) {
                for (int k = 0; k < getUsers().size(); k++) {
                    GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 177, 1, 500);
                    break;
                }
            }

            if (p.nj.ItemBodyHide[0] != null) {
                if (p.nj.ItemBodyHide[0].getUpgrade() == 6) {
                    switch (p.nj.ItemBodyHide[0].id) {
                        case 774: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 0,
                                //         1, 1);
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 1,
                                //         1, 1);
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 22, 1, 1);
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                //         (short) 23, 1, 1);
                            }
                            break;
                        }
                        case 786: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                //         (short) 25, 1, 1);
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 24, 1, 1);
                            }
                            break;
                        }
                        case 787: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 26, 1, 1);
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                //         (short) 27, 1, 1);
                            }
                            break;
                        }
                    }
                }
            }
            if (p.nj.ItemBodyHide[0] != null) {
                if (p.nj.ItemBodyHide[0].getUpgrade() == 6) {
                    switch (p.nj.ItemBodyHide[0].id) {
                        case 774: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 0,
                                //         1, 1);
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 1,
                                //         1, 1);
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 23, 1, 1);
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                //         (short) 23, 1, 1);
                            }
                            break;
                        }
                        case 786: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                //         (short) 25, 1, 1);
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 25, 1, 1);
                            }
                            break;
                        }
                        case 787: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 27, 1, 1);
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                //         (short) 27, 1, 1);
                            }
                            break;
                        }
                    }
                }
            }

            try {
                if (p.nj.get().ItemBody[31] != null && p.nj.get().ItemBody[31].id == 967) {
                    switch (p.nj.get().nclass) {
                        case 1: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 186, 1, 500);
                            }
                            break;
                        }
                        case 2: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 187, 1, 500);
                            }
                            break;
                        }
                        case 3: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 185, 1, 500);
                            }
                            break;
                        }
                        case 4: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 183, 1, 500);
                            }
                            break;
                        }
                        case 5: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 182, 1, 500);
                            }
                            break;
                        }
                        case 6: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 184, 1, 500);
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
//                if (p.nj.ItemBody[1] != null && p.nj.ItemBody[1].id == 1007) {
//                    for (int k = 0; k < this.getUsers().size(); ++k) {
//                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 210, 1000,
//                                1000);
//                    }
//                }
                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1008) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 212, 1000,
                                1000);
                    }
                }
                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1009) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 214, 1000,
                                1000);
                    }
                }
                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1010) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 216, 1000,
                                1000);
                    }
                }
                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1011) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 218, 1000,
                                1000);
                    }
                }
                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1012) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 195, 1000,
                                1000);
                    }
                }
                if (p.nj.ItemBody[12] != null && p.nj.ItemBody[12].id == 1013) {
                    Item item = p.nj.ItemBody[12];
                    if (item.getUpgrade() == 0) {
                        for (int k = 0; k < this.getUsers().size(); ++k) {
                            GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 209,
                                    4000, -1);
                        }
                    }
                }
                if (p.nj.get().ItemBody[29] != null && p.nj.get().ItemBody[29].id == 976) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                        // (short)104, 1, 500);
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 103, 1,
                                5000);
                        break;
                    }
                }
                if (p.nj.get().ItemBody[28] != null && p.nj.get().ItemBody[28].id == 982) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 207, 1,
                                500);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
//        try {
//                
//                
//                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1007) {
//                    for (int k = 0; k < this.getUsers().size(); ++k) {
//                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 210, 1000,
//                                1000);
//                    }
//                }
//                
//                
//                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1008) {
//                    for (int k = 0; k < this.getUsers().size(); ++k) {
//                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 212, 1000,
//                                1000);
//                    }
//                }
//                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1009) {
//                    for (int k = 0; k < this.getUsers().size(); ++k) {
//                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 214, 1000,
//                                1000);
//                    }
//                }
//                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1010) {
//                    for (int k = 0; k < this.getUsers().size(); ++k) {
//                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 216, 1000,
//                                1000);
//                    }
//                }
//                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1011) {
//                    for (int k = 0; k < this.getUsers().size(); ++k) {
//                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 218, 1000,
//                                1000);
//                    }
//                }
//                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1012) {
//                    for (int k = 0; k < this.getUsers().size(); ++k) {
//                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 195, 1000,
//                                1000);
//                    }
//                }
//                if (p.nj.ItemBody[12] != null && p.nj.ItemBody[12].id == 1013) {
//                    Item item = p.nj.ItemBody[12];
//                    if (item.getUpgrade() == 0) {
//                        for (int k = 0; k < this.getUsers().size(); ++k) {
//                            GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 209,
//                                    4000, -1);
//                        }
//                    }
//                }
//                if (p.nj.get().ItemBody[29] != null && p.nj.get().ItemBody[29].id == 976) {
//                    for (int k = 0; k < this.getUsers().size(); ++k) {
//                        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
//                        // (short)104, 1, 500);
//                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 103, 1,
//                                5000);
//                        break;
//                    }
//                }
//                if (p.nj.get().ItemBody[28] != null && p.nj.get().ItemBody[28].id == 982) {
//                    for (int k = 0; k < this.getUsers().size(); ++k) {
//                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 207, 1,
//                                500);
//                        break;
//                    }
//                }
//            } catch (Exception e) {
//            e.printStackTrace();
//        }

        // if (p.nj.get().fullTL() >= 7) {
        // if (System.currentTimeMillis() > p.nj.delayEffect) {
        // p.nj.delayEffect = System.currentTimeMillis() + 5000L;
        // byte tl = 0;
        // switch (GameScr.SysClass(p.nj.nclass)) {
        // case 1: {
        // tl = 9;
        // break;
        // }
        // case 2: {
        // tl = 3;
        // break;
        // }
        // case 3: {
        // tl = 6;
        // break;
        // }
        // }
        // if (p.nj.fullTL() >= 9) {
        // ++tl;
        // ++tl;
        // }
        // if (p.nj.fullTL() >= 7) {
        // tl += 0;
        // }
        //
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // tl, 1, 1);
        // }
        // }
        // }
        // if (p.nj.get().ItemBody[30].id == 976) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)113, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 833) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)87, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 834) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)88, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 835) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)89, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 836) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)90, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 837) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)91, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 838) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)92, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 839) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)93, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 840) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)94, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 841) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)95, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 842) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)96, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 843) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)97, 1, 500);
        // break;
        // }
        // }
        if (p.nj.get().mobMe != null && p.nj.get().mobMe.timeFight <= System.currentTimeMillis()) {
            this.loadMobMeAtk(p.nj);
        }

        updateSpecialEvent(p);
        if (this.map.isLangCo() && (p.nj.isDie || p.nj.pk > 0L)) {
            this.DieReturn(p);
        }

        if (System.currentTimeMillis() > p.nj.deleyRequestClan) {
            p.nj.requestclan = -1;
        }
        if (p != null
                && p.nj != null
                && p.nj.clone != null
                && p.nj.clone.isIslive() && System.currentTimeMillis() > p.nj.timeRemoveClone) {
            p.nj.clone.off();
        }

    }
@SneakyThrows
    private void updateUser(final @Nullable User p) throws IOException {
        if (p == null) {
            return;
        }
        if (p.nj.get().fullTL() >= 7) {
            if (System.currentTimeMillis() > p.nj.delayEffect) {
                if (p.nj.get().effectFlag) {
                    p.nj.delayEffect = System.currentTimeMillis() + 1000L;
                    byte tl = 0;
                    switch (GameScr.SysClass(p.nj.nclass)) {
                        case 1: {
                            tl = 9;
                            break;
                        }
                        case 2: {
                            tl = 9;
                            break;
                        }
                        case 3: {
                            tl = 9;
                            break;
                        }
                    }
                    if (p.nj.fullTL() == 9) {
                        tl += 2;
                    }
                    if (p.nj.fullTL() == 8) {
                        tl += 1;
                    }
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, tl, 1, 1);
                    }
                    if (p.nj.get().getNgocEff() != 0) {
                        p.nj.get().effectFlag = !p.nj.get().effectFlag;
                    }
                }
            }
        } else {
            p.nj.get().effectFlag = false;
        }
        if (System.currentTimeMillis() > p.nj.delayEffect) {
            if (p.nj.get().getNgocEff() != -4) {
                if (p.nj.get().fullTL() < 7) {
                    p.nj.get().effectFlag = false;
                }

                if (!p.nj.get().effectFlag) {
                    p.nj.delayEffect = System.currentTimeMillis() + 5000L;
                    int fullNgoc = p.nj.get().getNgocEff();
                    if (fullNgoc == -2) {
                        short iClass = 0;
                        switch (GameScr.SysClass(p.nj.nclass)) {
                            case 1: {
                                iClass = 9;
                                break;
                            }
                            case 2: {
                                iClass = 3;
                                break;
                            }
                            case 3: {
                                iClass = 6;
                                break;
                            }
                        }
                        for (int k = 0; k < this.getUsers().size(); ++k) {
                            GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                    (short) iClass, 1, 1);
                        }
                    }
                    if (fullNgoc == -1) {
                        short iClass = 0;
                        switch (GameScr.SysClass(p.nj.nclass)) {
                            case 1: {
                                iClass = 10;
                                break;
                            }
                            case 2: {
                                iClass = 4;
                                break;
                            }
                            case 3: {
                                iClass = 7;
                                break;
                            }
                        }
                        for (int k = 0; k < this.getUsers().size(); ++k) {
                            GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                    (short) iClass, 1, 1);
                        }
                    }
                    if (fullNgoc == 1) {
                        short iClass = 0;
                        switch (GameScr.SysClass(p.nj.nclass)) {
                            case 1: {
                                iClass = 11;
                                break;
                            }
                            case 2: {
                                iClass = 5;
                                break;
                            }
                            case 3: {
                                iClass = 8;
                                break;
                            }
                        }
                        for (int k = 0; k < this.getUsers().size(); ++k) {
                            GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                    (short) iClass, 1, 1);
                        }
                    }

                    // if (fullNgoc < 0) {
                    // fullNgoc += 3;
                    // for (int k = 0; k < this.getUsers().size(); ++k) {
                    // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                    // (short) fullNgoc, 2000, 1);
                    // }
                    // } else if (fullNgoc <= 3) {
                    // if (fullNgoc > 2) {
                    // fullNgoc = 2;
                    // }
                    // short iClass = 0;
                    // switch (GameScr.SysClass(p.nj.nclass)) {
                    // case 1: {
                    // iClass = 9;
                    // break;
                    // }
                    // case 2: {
                    // iClass = 3;
                    // break;
                    // }
                    // case 3: {
                    // iClass = 6;
                    // break;
                    // }
                    // }
                    // for (int k = 0; k < this.getUsers().size(); ++k) {
                    // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                    // (short) (fullNgoc + iClass), 1, 1);
                    // }
                    // } else {
                    // }
                    if (p.nj.get().fullTL() >= 7) {
                        p.nj.get().effectFlag = !p.nj.get().effectFlag;
                    }
                }
            } else {
                p.nj.get().effectFlag = true;
            }
        }
        if (p.nj.get().getNgocEff() == 3) {
            short iClass = 0;
            switch (GameScr.SysClass(p.nj.nclass)) {
                case 1: {
                    iClass = 204;
                    break;
                }
                case 2: {
                    iClass = 206;
                    break;
                }
                case 3: {
                    iClass = 208;
                    break;
                }
            }
            for (int k = 0; k < this.getUsers().size(); ++k) {

                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) (iClass), 10000,
                        -1);
            }
        }
        synchronized (p) {
            removeIfItemExpired(p);
        }
        if (this.map.id == 160 || this.map.id == 162) {// vithu
            final Calendar rightNow = Calendar.getInstance();
            short moment = (short) rightNow.get(Manager.BOSS_WAIT_TIME_UNIT);
            if (this.id == 0) {
                p.nj.setTypepk((short) 0);
                p.nj.changeTypePkNormal((short) 0);
            }
            if (moment != 21) {
                this.DieReturn(p);
            }
        }
        if ((this.map.isLangCo() || this.map.id == 171 || this.map.id == 172 || this.map.id == 173)
                && (p.nj.isDie || p.nj.pk > 0L)) {
            this.DieReturn(p);
        }
        if (this.map.id == 169 && (p.nj.isDie || p.nj.pk > 0L || p.nj.get().getEffId(43) == null || !p.isSVip)) {// ||
            // !p.isSVip
            this.DieReturn(p);
        }
        try {
            for (byte i = 0; i < p.nj.ItemBag.length; i++) {
                Item item = p.nj.ItemBag[i];
                if (item != null && item.quantity < 0) {
                    item.setLock(true);
                    item.quantity = 1;
                }
            }

            // xoa item tui het han cua ng choi
            for (byte j = 0; j < p.nj.ItemBag.length; j++) {
                Item item = p.nj.ItemBag[j];
                if (item == null || !item.isExpires) {
                    continue;
                }
                if (System.currentTimeMillis() >= item.expires) {
                    p.nj.removeItemBag(j, item.quantity);
                }
            }
            // xoa item tra bi het han cua ng choi
            for (byte j = 0; j < p.nj.get().ItemBody.length; j++) {
                Item item = p.nj.get().ItemBody[j];
                if (item == null || !item.isExpires) {
                    continue;
                }
                if (System.currentTimeMillis() >= item.expires) {
                    if (p.nj.isHuman) {
                        p.nj.removeItemBody(j);
                    } else if (p.nj.isNhanban) {
                        p.nj.clone.removeItemBody(j);
                    }
                }
            }
            // xoa item ruong het han cua ng choi
            for (byte j = 0; j < p.nj.ItemBox.length; j++) {
                Item item = p.nj.ItemBox[j];
                if (item == null || !item.isExpires) {
                    continue;
                }
                if (System.currentTimeMillis() >= item.expires) {
                    p.nj.removeItemBox(j);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // if((this.map.isLangCo() || map.id == 75 || map.id == 76 || map.id == 77 ||
        // map.id == 79) && (p.nj.isDie || p.nj.pk > 0)) {
        // p.nj.place.leave(p);
        // Map ma = Manager.getMapid(p.nj.mapLTD);
        // byte k;
        // for (k = 0; k < ma.area.length; k++) {
        // if (ma.area[k].getNumplayers() < ma.template.maxplayers) {
        // ma.area[k].EnterMap0(p.nj);
        // return;
        // }
        // }
        // }
        updateEffect(p);
        try {
            if (System.currentTimeMillis() > p.nj.delayEffect2) {
                if (p.nj.get().getNgocEff() != 0) {
                    p.nj.delayEffect2 = System.currentTimeMillis() + 5000L;
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        if (p.nj.get().getNgocEff() >= 100) {
                            GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 5, 1,
                                    1);
                            GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 11, 1,
                                    1);
                        } else {
                            GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                    (short) p.nj.get().getNgocEff(), 1, 1);
                        }
                    }
                }
            }

            // if (p.nj.time136 >= System.currentTimeMillis()) {
            // for (int k = 0; k < this.getUsers().size(); ++k) {
            // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
            // (short)73, 1, 1);
            // }
            // }
            // if (p.nj.name.equals("admin")) {
            // for (int k = 0; k < this.getUsers().size(); ++k) {
            // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
            // (short)99, 1, 500);
            // }
            // }
            // if (p.nj.name.equals("admin")) {
            // for (int k = 0; k < this.getUsers().size(); ++k) {
            // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
            // (short)98, 1, 500);
            // }
            // }
            // if (p.nj.name.equals("admin") || p.nj.name.equals("") || p.nj.name.equals("")
            // || p.nj.name.equals("") || p.nj.name.equals("") || p.nj.name.equals("") ||
            // p.nj.name.equals("")) {
            // for (int k = 0; k < getUsers().size(); k++) {
            // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
            // (short)159, 1, 500);
            //// GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0,
            // p.nj.get().id, (short) 120, 1, 500);
            //// GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0,
            // p.nj.get().id, (byte) 120, 500, 1);
            //// GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0,
            // p.nj.get().id, (short) 5, 21000000, 10);
            //// GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0,
            // p.nj.get().id, (short) 8, 21050000, 5);
            //// GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0,
            // p.nj.get().id, (short) 11, 20050000, 3);
            // }
            // }
            if (p.tongnap > 1000000) {
                for (int k = 0; k < getUsers().size(); k++) {
                    GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 177, 1, 500);
                    break;
                }
            }

            if (p.nj.ItemBodyHide[0] != null) {
                if (p.nj.ItemBodyHide[0].getUpgrade() == 6) {
                    switch (p.nj.ItemBodyHide[0].id) {
                        case 774: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 0,
                                //         1, 1);
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 1,
                                //         1, 1);
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 22, 1, 1);
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                //         (short) 23, 1, 1);
                            }
                            break;
                        }
                        case 786: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                //         (short) 25, 1, 1);
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 24, 1, 1);
                            }
                            break;
                        }
                        case 787: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 26, 1, 1);
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                //         (short) 27, 1, 1);
                            }
                            break;
                        }
                    }
                }
            }
            if (p.nj.ItemBodyHide[0] != null) {
                if (p.nj.ItemBodyHide[0].getUpgrade() == 6) {
                    switch (p.nj.ItemBodyHide[0].id) {
                        case 774: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 0,
                                //         1, 1);
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 1,
                                //         1, 1);
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 23, 1, 1);
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                //         (short) 23, 1, 1);
                            }
                            break;
                        }
                        case 786: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                //         (short) 25, 1, 1);
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 25, 1, 1);
                            }
                            break;
                        }
                        case 787: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 27, 1, 1);
                                // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                //         (short) 27, 1, 1);
                            }
                            break;
                        }
                    }
                }
            }

            try {
                if (p.nj.get().ItemBody[31] != null && p.nj.get().ItemBody[31].id == 967) {
                    switch (p.nj.get().nclass) {
                        case 1: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 186, 1, 500);
                            }
                            break;
                        }
                        case 2: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 187, 1, 500);
                            }
                            break;
                        }
                        case 3: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 185, 1, 500);
                            }
                            break;
                        }
                        case 4: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 183, 1, 500);
                            }
                            break;
                        }
                        case 5: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 182, 1, 500);
                            }
                            break;
                        }
                        case 6: {
                            for (int k = 0; k < this.getUsers().size(); ++k) {
                                GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                                        (short) 184, 1, 500);
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                if (p.nj.ItemBody[10] != null && p.nj.ItemBody[10].id == 956) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 78, 1000,
                                1000);
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 87, 1000,
                                1000);
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 74, 1000,
                                1000);
                        
                        
               
                    }
                }
                    if (p.nj.ItemBody[10] != null && p.nj.ItemBody[10].id == 953) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 82, 1000,
                                1000);
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 91, 1000,
                                1000);
                        
               
                    }
                }
                
                if (p.nj.ItemBody[10] != null && p.nj.ItemBody[10].id == 954) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 205, 1000,
                                1000);
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 83, 1000,
                                1000);
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 92, 1000,
                                1000);
                        
                        
               
                    }
                }
                if (p.nj.ItemBody[10] != null && p.nj.ItemBody[10].id == 957) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 95, 1000,
                                1000);
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 86, 1000,
                                1000);
                        
               
                    }
                }
                
                
                if (p.nj.ItemBody[10] != null && p.nj.ItemBody[10].id == 937) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 84, 1000,
                                1000);
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 93, 1000,
                                1000);
                        
               
                    }
                }
                
                
                
                
                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1007) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 210, 1000,
                                1000);
                    }
                }

                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1008) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 212, 1000,
                                1000);
                    }
                }
                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1009) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 214, 1000,
                                1000);
                    }
                }
                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1010) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 216, 1000,
                                1000);
                    }
                }
                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1011) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 218, 1000,
                                1000);
                    }
                }
                if (p.nj.ItemBody[31] != null && p.nj.ItemBody[31].id == 1012) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 195, 1000,
                                1000);
                    }
                }
                if (p.nj.ItemBody[12] != null && p.nj.ItemBody[12].id == 1013) {
                    Item item = p.nj.ItemBody[12];
                    if (item.getUpgrade() == 0) {
                        for (int k = 0; k < this.getUsers().size(); ++k) {
                            GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 209,
                                    4000, -1);
                        }
                    }
                }
                if (p.nj.get().ItemBody[29] != null && p.nj.get().ItemBody[29].id == 976) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
                        // (short)104, 1, 500);
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 103, 1,
                                5000);
                        break;
                    }
                }
                if (p.nj.get().ItemBody[28] != null && p.nj.get().ItemBody[28].id == 982) {
                    for (int k = 0; k < this.getUsers().size(); ++k) {
                        GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id, (short) 207, 1,
                                500);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (p.nj.getPramItem(99) > 0 && p.nj.eff05buff <= System.currentTimeMillis()) {
                p.nj.eff05buff = System.currentTimeMillis() + 500L;
                p.nj.get().upHP(p.nj.getPramItem(99));
                p.nj.get().upMP(p.nj.getPramItem(99));
                MessageSubCommand.sendHP(p.nj.get(), getUsers());
                MessageSubCommand.sendMP(p.nj.get(), getUsers());
            }
            if ((p.nj.eff5buffHP() > 0 || p.nj.get().eff5buffMP() > 0) && p.nj.eff5buff <= System.currentTimeMillis()) {
                p.nj.eff5buff = System.currentTimeMillis() + 5000L;
                p.nj.get().upHP(p.nj.get().eff5buffHP());
                p.nj.get().upMP(p.nj.get().eff5buffMP());
                MessageSubCommand.sendHP(p.nj.get(), getUsers());
                MessageSubCommand.sendMP(p.nj.get(), getUsers());
            }
            if ((p.nj.eff10buffHP() > 0) && p.nj.eff10buff <= System.currentTimeMillis()) {
                p.nj.eff10buff = System.currentTimeMillis() + 10000L;
                p.nj.get().upHP(p.nj.get().eff10buffHP());
                MessageSubCommand.sendHP(p.nj.get(), getUsers());
                MessageSubCommand.sendMP(p.nj.get(), getUsers());
            }

            if (p.nj.getPramItem(136) > 0 && p.nj.eff136 <= System.currentTimeMillis()) {
                p.nj.eff136 = System.currentTimeMillis() + 40000L;
                if (util.nextInt(1, 100) <= 10) {
                    p.nj.time136 = System.currentTimeMillis() + 5000L;
                    p.sendYellowMessage("Miá»…n thÆ°Æ¡ng thÃ nh cÃ´ng");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // if (p.nj.get().fullTL() >= 7) {
        // if (System.currentTimeMillis() > p.nj.delayEffect) {
        // p.nj.delayEffect = System.currentTimeMillis() + 5000L;
        // byte tl = 0;
        // switch (GameScr.SysClass(p.nj.nclass)) {
        // case 1: {
        // tl = 9;
        // break;
        // }
        // case 2: {
        // tl = 3;
        // break;
        // }
        // case 3: {
        // tl = 6;
        // break;
        // }
        // }
        // if (p.nj.fullTL() >= 9) {
        // ++tl;
        // ++tl;
        // }
        // if (p.nj.fullTL() >= 7) {
        // tl += 0;
        // }
        //
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // tl, 1, 1);
        // }
        // }
        // }
        // if (p.nj.get().ItemBody[30].id == 976) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)113, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 833) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)87, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 834) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)88, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 835) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)89, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 836) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)90, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 837) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)91, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 838) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)92, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 839) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)93, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 840) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)94, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 841) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)95, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 842) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)96, 1, 500);
        // }
        // }
        // if (p.nj.get().ItemBody[10].id == 843) {
        // for (int k = 0; k < this.getUsers().size(); ++k) {
        // GameCanvas.addEffect(this.getUsers().get(k).session, (byte) 0, p.nj.get().id,
        // (short)97, 1, 500);
        // break;
        // }
        // }
        if (p.nj.get().mobMe != null && p.nj.get().mobMe.timeFight <= System.currentTimeMillis()) {
            this.loadMobMeAtk(p.nj);
        }

        updateSpecialEvent(p);
        if (this.map.isLangCo() && (p.nj.isDie || p.nj.pk > 0L)) {
            this.DieReturn(p);
        }

        if (System.currentTimeMillis() > p.nj.deleyRequestClan) {
            p.nj.requestclan = -1;
        }
        if (p != null
                && p.nj != null
                && p.nj.clone != null
                && p.nj.clone.isIslive() && System.currentTimeMillis() > p.nj.timeRemoveClone) {
            p.nj.clone.off();
        }

    }

    private List<ItemMap> findItemMapInDistance(int x, int y, int distance, boolean filter, int master) {
        List<ItemMap> itemMaps = new ArrayList<>();
        for (ItemMap itemMap : this._itemMap) {
            if (itemMap != null && (itemMap.master == -1 || itemMap.master == master)
                    && Math.sqrt(Math.pow(itemMap.x - x, 2) + Math.pow(itemMap.y - y, 2)) <= distance) {
                itemMaps.add(itemMap);
            }
        }
        return itemMaps;
    }

    private Mob findMobInDistance(int x, int y, int distance) {
        for (Mob mob : this._mobs) {
            if (mob != null && !mob.isDie && Math.sqrt(Math.pow(mob.x - x, 2) + Math.pow(mob.y - y, 2)) <= distance) {
                return mob;
            }
        }
        return null;
    }

    @SneakyThrows
    private void updateHpToFriend(final @Nullable User p) {
        if (p == null) {
            return;
        }

        val m = messageSubCommand2(17);
        m.writer().writeInt(p.nj.id);
        m.writer().writeInt(p.nj.hp);
        m.writer().flush();
        sendMessage(m);
        m.cleanup();
    }

    public void updateMp(final @Nullable User p) throws IOException {
        if (p == null) {
            return;
        }
        Message msg = null;
        try {
            msg = new Message(-30);
            msg.writer().writeByte(-121);
            msg.writer().writeInt(p.nj.mp);
            p.sendMessage(msg);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private void updateHp(final @Nullable User p) throws IOException {
        if (p == null) {
            return;
        }
        Message msg = null;
        try {
            msg = new Message(-30);
            msg.writer().writeByte(-122);
            msg.writer().writeInt(p.nj.hp);
            p.sendMessage(msg);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private void updateExpiredItemMap() throws IOException {
        for (int i = 0; i < this._itemMap.size(); ++i) {
            final ItemMap itm = this._itemMap.get(i);
            if (itm == null || itm.item == null) {
                continue;
            }
            if (itm.removedelay != -1
                    && System.currentTimeMillis() >= itm.removedelay) {
                this.removeItemMapMessage(itm.itemMapId);
                this._itemMap.remove(i);
                --i;
            } else if (itm.removedelay - System.currentTimeMillis() < 45000L && itm.master != -1) {
                itm.master = -1;
            }
        }
    }

    private void updateEffect(final @Nullable User p) {
        if (p == null) {
            return;
        }
        for (Effect eff : p.nj.get().getVeff()) {
            if (System.currentTimeMillis() >= eff.timeRemove) {
                p.removeEffect(eff.template.id);

            } else {
                eff.timeStart++;
            }
            if (eff.template.type == 0 || eff.template.type == 12) {
                p.nj.get().upHP(eff.param);
                p.nj.get().upMP(eff.param);
            } else if (eff.template.type == 4 || eff.template.type == 17) {
                p.nj.get().upHP(eff.param);
            } else if (eff.template.type == 13) {
                p.nj.get().upHP(-(p.nj.get().getMaxHP() * 3 / 100));
                if (p.nj.get().isDie) {
                    p.nj.get().upDie();
                }
            }
        }
    }

    private void removeIfItemExpired(final @Nullable User p) throws IOException {
        if (p == null) {
            return;
        }
        for (int l = 0; l < p.nj.ItemBag.length; ++l) {
            final Item item = p.nj.ItemBag[l];
            if (item != null) {
                if (item.isExpires) {
                    if (item.isExpired()) {
                        p.nj.removeItemBag(l, item.quantity);
                    }
                }
            }
        }
        for (byte l = 0; l < p.nj.get().ItemBody.length; ++l) {
            final Item item = p.nj.get().ItemBody[l];
            if (item != null) {
                if (item.isExpires) {
                    if (item.isExpired()) {
                        p.nj.removeItemBody(l);
                    }
                }
            }
        }
        for (byte l = 0; l < p.nj.ItemBox.length; ++l) {
            final Item item = p.nj.ItemBox[l];
            if (item != null) {
                if (item.isExpires) {
                    if (item.isExpired()) {
                        p.nj.removeItemBox(l);
                    }
                }
            }
        }
    }
    // </editor-fold>

    @SneakyThrows
    public void updateSpecialEvent(final @Nullable User p) {

        if (p == null) {
            return;
        }
        val nj = p.nj;

        TeamBattle t = nj.party == null ? nj : nj.party;
        if (t.hasBattle()) {
            if (t.getBattle().isExpired()) {
                Map map = server.getMaps()[27];
                Place area = map.getFreeArea();
                t.enterSamePlace(area, null);
            } else if (t.getBattle().getState() == Battle.CHIEN_DAU_STATE) {
                if (t.loose()) {
                    t.getBattle().updateWinner(t);
                }
            }

            if (Battle.BATTLE_Y_RANGE_MIN > nj.y && Battle.BATTLE_Y_RANGE_MAX < nj.y) {
                nj.y = (short) Battle.BATTLE_Y_RANGE_MAX;
            }
        }

        if (battle != null) {
            try {
                battle.update(nj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (UpdateEvent runnable : this.runner) {
            try {
                runnable.update(nj);
            } catch (Exception e) {
                e.printStackTrace();
                runner.remove(runnable);
            }
        }

        try {
            if (nj.isHuman) {
                if (nj.clone != null && nj.clone.isIslive() && nj.clone.nclass == 6) {
                    val skills = nj.clone.getWinBuffSkills();
                    val winBuffSkill = skills[util.nextInt(skills.length)];
                    if (winBuffSkill != -1 && nj.clone.getSkill(winBuffSkill).coolDown < System.currentTimeMillis()) {
                        useSkill.useSkillCloneBuff(nj.clone, winBuffSkill);
                        cloneBuffPlayer(p, winBuffSkill);// hiệu ứng skill
                        nj.clone.getSkill(winBuffSkill).coolDown = System.currentTimeMillis()
                                + nj.clone.getSkill(winBuffSkill).getTemplate().coolDown;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!p.containsItem(572)) {
            p.typeTBLOption = NOT_USE;

        } else {

            if (p.typeTBLOption == $240 || p.typeTBLOption == $480 || p.typeTBLOption == ALL_MAP) {
                List<ItemMap> itemMaps = findItemMapInDistance(p.nj.get().x, p.nj.get().y, 100, p.filter,
                        p.nj.get().id);

                for (ItemMap itemMap : itemMaps) {
                    if (p.nj.getAvailableBag() != 0 && itemMap != null && itemMap.item != null
                            && itemMap.item.getData().type != 25) {
                        removeItemMap(p, (short) _itemMap.indexOf(itemMap), itemMap);
                    }
                }
            }

            if (p.activeTBL) {
                final Mob mobInDistance = findMobInDistance(p.nj.get().x, p.nj.get().y, p.typeTBLOption.getValue());
                if (mobInDistance != null) {
                    sendXYPlayerWithEffect(p, p.nj.get().x, p.nj.get().y);
                    p.nj.get().x = mobInDistance.x;
                    boolean typeFly = mobInDistance.templates.type == 4;
                    p.nj.get().y = typeFly ? (short) (mobInDistance.y - 25) : mobInDistance.y;
                    sendXYPlayer(p);
                }
            }
        }
        // Nhiem vu heo rung
        if (map.id == 74 && p != null && p.nj != null && p.nj.get() != null
                && p.nj.get().isDie) {
            gietHeoRungGoBack(p, "Nhiệm vụ thất bại do hít quá nhiều khí độc");
        }
    }

    private void gietHeoRungGoBack(final @Nullable User p, final @Nullable String message) throws IOException {
        if (p == null || message == null) {
            return;
        }

        leave(p);
        p.expiredTime = -1;

        final Place freeArea = Server.getMapById(8).getFreeArea();
        if (freeArea != null) {
            for (Npc npc : freeArea.map.template.npc) {
                if (npc != null && npc.id == 15) {
                    p.nj.x = npc.x;
                    p.nj.y = npc.y;
                    break;
                }
            }
            p.nj.get().isDie = false;
            p.nj.get().upHP(p.nj.get().getMaxHP());
            p.nj.get().upMP(p.nj.get().getMaxMP());
            MessageSubCommand.sendHP(p.nj.get(), getUsers());
            MessageSubCommand.sendMP(p.nj.get(), getUsers());
            Service.batDauTinhGio(p, 0);
            val m = new Message(57);
            m.writer().flush();
            p.sendMessage(m);
            m.cleanup();
            p.nj.setMapid(8);
            freeArea.Enter(p);

        } else {
            gotoHaruna(p);
        }
        p.sendYellowMessage(message);
    }

    public void close() {
        this._users.clear();
    }

    public byte getNumplayers() {
        return (byte) this.getUsers().size();
    }

    public synchronized void removeRunner(UpdateEvent runnable) {
        this.runner.remove(runnable);
    }

    public synchronized Place addRunner(UpdateEvent runnable) {
        this.runner.add(runnable);
        return this;
    }

    @SneakyThrows
    public void sendPlayersInfo(final @Nullable Ninja nj, final @Nullable Message message) {
        if (nj == null || message == null) {
            return;
        }
        val m = new Message(25);
        val ds = m.writer();
        val size = message.reader().readByte();
        for (int i = 0; i < size; i++) {
            val ninja = getNinja(message.reader().readInt());
            if (ninja == null) {
                continue;
            }
            ds.writeInt(ninja.id);
            ds.writeShort(ninja.x);
            ds.writeShort(ninja.y);
            ds.writeInt(ninja.hp);
        }
        ds.flush();
        sendMessage(m);
        message.cleanup();
        m.cleanup();
    }

    private void MobAtkBuNhinMessage(final int mobid, final short idBuNhin, final short idskill_atk, final byte typeatk,
            final byte typetool) throws IOException {
        Message m = new Message(76);
        m.writer().writeByte(mobid);
        m.writer().writeShort(idBuNhin);
        m.writer().writeShort(idskill_atk);
        m.writer().writeByte(typeatk);
        m.writer().writeByte(typetool);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    public void addBuNhin(final @NotNull BuNhin buNhin) {
        buNhins.add(buNhin);
        MessageSubCommand.sendBuNhin(buNhin, getUsers());
    }

    public void removeBuNhin(@NotNull final BuNhin buNhin) {
        final int b = buNhins.indexOf(buNhin);
        buNhins.remove(buNhin);
        MessageSubCommand.removeBuNhin(b, getUsers());
    }

    public List<@Nullable User> getUsers() {
        return this._users;
    }

    public List<Mob> getMobs() {
        return _mobs;
    }

    private boolean canEnter = true;

    public boolean canEnter() {
        return canEnter;
    }

    @NotNull
    public Place open() {
        this.canEnter = true;
        return this;
    }

    public void reset() {
        numTA = 0;
        numTL = 0;
        recoverTa = false;
        canEnter = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Place place = (Place) o;
        return id == place.id && Objects.equals(map, place.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, map);
    }

    public void terminate() {
        for (User user : this._users) {
            if (user != null && user.nj != null) {
                try {
                    this.gotoHaruna(user);
                } catch (Exception e) {

                }
            }
        }
        this._users.clear();
        this.runner.clear();
        this._itemMap.clear();
        this._mobs.clear();
    }

    public Place setCandyBattle(@Nullable CandyBattle candyBattle) {
        this.candyBattle = candyBattle;
        return this;
    }

    public CandyBattle getCandyBattle() {
        return candyBattle;
    }

    public void killMob(int id) {
        for (Mob mob : this._mobs) {
            if (mob != null && mob.id == id) {
                mob.updateHP(-mob.hpmax);
                mob.isDie = true;
            }
        }
    }

    public void attack(List<Ninja> collect) {

    }

    public void cloneBuffPlayer(User p, int CSkill) throws IOException {
        Message m = new Message(61);
        m.writer().writeInt(p.nj.clone.id);
        m.writer().writeByte(CSkill);
        m.writer().writeInt(p.nj.id);
        m.writer().flush();
        sendMessage(m);
        // this.sendMyMessage(p, m);
        m.cleanup();
    }

    public void callmob(short vgox, short vgoy, User p, short idmob) {
        try {
            byte id = (byte) _mobs.size();
            short[] arId = new short[]{idmob};
            short mob = arId[util.nextInt(arId.length)];
            Mob mobnew = new Mob(id, mob, p.nj.get().getLevel());
            mobnew.templates.id = mob;
            mobnew.x = vgox;
            mobnew.y = vgoy;
            mobnew.hpmax = 1000;
            mobnew.hp = 1000;
            mobnew.status = 5;
            mobnew.lvboss = 0;
            mobnew.setIsboss(true);
            mobnew.isIsboss();
            mobnew.isDisable = false;
            mobnew.isDontMove = false;
            mobnew.isFire = false;
            mobnew.isIce = false;
            mobnew.isWind = false;
            mobnew.sys = (byte) util.nextInt(1, 3);
            mobnew.tuanlocid = p.nj.get().id;
            this.getMobs().add(mobnew);
            this.sendMapInfo(p, this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public ItemMap LeaveItem(final Place place, final int id, int njX, int njY, int quantity) throws IOException {// vithu

        int rand = 0;
        if (id == 457) {
            rand = util.nextInt(0, 2);
            if (rand == 2) {
                return null;
            }
        }

        if (place._itemMap.size() > 100) {
            place.removeItemMapMessage(place._itemMap.remove(0).itemMapId);
        }

        final ItemData data = ItemDataId(id);
        if (data == null) {
            return null;
        }
        Item item;
        if (data.type < 10) {
            if (data.type == 1) {
                item = itemDefault(id);
                item.sys = GameScr.SysClass(data.nclass);
            } else {
                final byte sys = (byte) util.nextInt(1, 3);
                item = itemDefault(id, sys);
            }
        } else {
            item = itemDefault(id);
        }
        if (item.isTypeNgocKham() || item.isTypeBody()) {
            for (Option option : item.option) {
                option.param = util.nextInt(option.param * 70 / 100, option.param);
            }
        }
        final ItemMap im = new ItemMap();
        im.itemMapId = place.getItemMapNotId();
        im.removedelay = 600000L + System.currentTimeMillis();
        im.x = (short) util.nextInt(njX - 2, njX + 2);
        im.y = (short) njY;
        im.item = item;
        item.quantity = quantity;
        place._itemMap.add(im);
        final Message m = new Message(6);
        m.writer().writeShort(im.itemMapId);
        m.writer().writeShort(item.id);
        m.writer().writeShort(im.x);
        m.writer().writeShort(im.y);
        m.writer().flush();
        place.sendMessage(m);
        m.cleanup();
        return im;
    }
}
