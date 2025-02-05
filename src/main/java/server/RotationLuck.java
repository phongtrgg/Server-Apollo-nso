package server;

import real.Ninja;
import real.PlayerManager;
import real.User;
import threading.Manager;
import threading.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class RotationLuck extends Thread {

    protected String title;
    protected short time;
    protected int total;
    protected int maxTotal;
    protected short numPlayers;
    protected byte type;
    protected final int min;
    protected final int max;
    protected boolean open;
    protected short setTime;
    protected boolean start;
    protected String winnerInfo;
    protected boolean running;
    protected ArrayList<Players> players;
    protected String currency;

    public RotationLuck(final String title, final byte type, final short time, final int min, final int max, final int maxtotal, String currency) {

        this.title = null;
        this.time = 120;
        this.total = 0;
        this.numPlayers = 0;
        this.type = 1;
        this.open = true;
        this.setTime = 0;
        this.start = false;
        this.winnerInfo = "Chưa có thông tin";
        this.running = true;
        this.players = new ArrayList<>();
        this.title = title;
        this.type = type;
        this.setTime = time;
        this.time = time;
        this.min = min;
        this.max = max;
        this.maxTotal = maxtotal;
        this.currency = currency;

    }

    public RotationLuck(final String title, final byte type, final short time, final int min, final int max, final int maxtotal) {
        this(title, type, time, min, max, maxtotal, "xu");
    }

    protected int getJoinAmount(final String njname) {

        for (short i = 0; i < this.players.size(); ++i) {
            if (this.players.get(i).name.equals(njname)) {
                return this.players.get(i).joinAmount;
            }
        }
        return 0;
    }

    public int getNinjaAmount(User user) {
        if (currency.equals("xu")) {
            return user.nj.xu;
        } else {
            return user.nj.xu;
            //return user.luong;
        }
    }

    public synchronized void upAmountMessage(long amount, User user) {
        if (currency.equals("xu")) {
            user.nj.upxuMessage(amount);
        } else {
            user.nj.upxuMessage(amount);
        }
    }

    protected synchronized void joinLuck(final User p, final int joinAmount) throws IOException {
        if (!this.open || joinAmount <= 0) {
            return;
        }
//        if (p.tongnap < 10000) {
//            p.session.sendMessageLog("Yêu cầu nạp tối thiểu 10k để chơi vòng xoay may mắn");
//            return;
//        }
        
        if (p.nj.xu - joinAmount < 10000000 && this.type == 1) {
            p.session.sendMessageLog("Bạn phải còn tối thiểu 10 triệu xu trong người mới có thể tham gia vòng xoay.");
            return;
        }
        if (joinAmount > getNinjaAmount(p)) {
            p.session.sendMessageLog("Bạn không đủ " + currency + ".");
            return;
        }
        if (this.total > this.maxTotal) {
            p.session.sendMessageLog("Số lượng " + currency + " tối đa là " + util.getFormatNumber(this.maxTotal));
            return;
        }
        Players p2 = null;
        for (short i = 0; i < this.players.size(); ++i) {
            if (this.players.get(i).name.equals(p.nj.name)) {
                p2 = this.players.get(i);
                break;
            }
        }
        if (p2 == null && (joinAmount > this.max || joinAmount < this.min)) {
            p.session.sendMessageLog("Bạn chỉ có thể đặt cược từ " + util.getFormatNumber(this.min) + " đến " + util.getFormatNumber(this.max) + currency + ".");
            return;
        }
        if (p2 == null) {
            p2 = new Players(p.id);
            p2.user = p.username;
            p2.name = p.nj.name;
            ++this.numPlayers;
            this.players.add(p2);
        }
        if (p2.joinAmount + joinAmount > this.max) {
            p.session.sendMessageLog("Bạn chỉ có thể đặt tối đa " + util.getFormatNumber(this.max - p2.joinAmount) + ".");
            return;
        }
        if(p.nj.isTaskDanhVong == 1  && p.nj.nhiemvuDV && (p.nj.taskDanhVong[0] == 0 && joinAmount >= 1000000 && joinAmount <= 50000000) || (p.nj.taskDanhVong[0] == 1 && joinAmount >= 10000 && joinAmount <= 100000)) {
            p.nj.taskDanhVong[1]++;
            if(p.nj.taskDanhVong[1] == p.nj.taskDanhVong[2]) {
                p.sendYellowMessage("Bạn đã hoàn thành nhiệm vụ danh vọng.");
            }
        }
        final Players players = p2;
        players.joinAmount += joinAmount;
        upAmountMessage(-joinAmount, p);
        this.total += joinAmount;
        if (this.numPlayers == 2 && !this.start) {
            this.begin();
            final Ninja ns = PlayerManager.getInstance().getNinja(this.players.get(0).name);
            if (ns != null) {
                this.luckMessage(ns.p);
            }
        }
        this.luckMessage(p);
    }

    private void turned() throws Exception {

        for (int i = 0; i < this.players.size(); ++i) {
            for (int j = i + 1; j < this.players.size(); ++j) {
                if (this.players.get(i).joinAmount < this.players.get(j).joinAmount) {
                    final String tempuser = this.players.get(j).user;
                    final String tempname = this.players.get(j).name;
                    final int tempAmount = this.players.get(j).joinAmount;
                    this.players.get(j).user = this.players.get(i).user;
                    this.players.get(j).name = this.players.get(i).name;
                    this.players.get(j).joinAmount = this.players.get(i).joinAmount;
                    this.players.get(i).user = tempuser;
                    this.players.get(i).name = tempname;
                    this.players.get(i).joinAmount = tempAmount;
                }
            }
        }
        Players p = null;

        if (util.nextInt(1,100) < 50) {
            for (final Players player : this.players) {
                if (this.percentWin(player.name) > util.nextInt(100)) {
                    p = player;
                    break;
                }
            }
        } else {
            for (int i = this.players.size() - 1; i >= 0; i--) {
                if (this.percentWin(this.players.get(i).name) > util.nextInt(100)) {
                    p = this.players.get(i);
                    break;
                }
            }
        }
        if (p == null) {
            p = this.players.get(util.nextInt(this.players.size()));
        }


        long amountWin = this.total;
        if (this.numPlayers > 1) {
            amountWin = amountWin * 95L / 100L;
        }
        this.numPlayers = 0;
        this.total = 0;
        final Ninja ns = PlayerManager.getInstance().getNinja(p.name);
        if (ns != null) {
            upAmountMessage(amountWin, ns.p);
        } else {
            if (currency.equals("xu")) {
                SQLManager.executeUpdate("UPDATE `ninja` SET `xu`=`xu`+" + amountWin + " WHERE `name`='" + p.name + "';");
            } else {
                SQLManager.executeUpdate("UPDATE `ninja` SET `xu`=`xu`+" + amountWin + " WHERE `id`=" + p.id + ";");
            }
        }
        Manager.serverChat("Server", "Chúc mừng " + p.name.toUpperCase() + " đã chiến thắng " + util.getFormatNumber(amountWin) + " " + currency + " trong trò chơi Vòng xoay may mắn");
        this.winnerInfo = "Người vừa chiến thắng:\n" + ((this.type == 0) ? ("c" + util.nextInt(10)) : "") + "" + p.name + "\nSố " + currency + " thắng: " + util.getFormatNumber(amountWin) + " " + currency + "\nSố " + currency + " tham gia: " + util.getFormatNumber(p.joinAmount) + " " + currency;
        this.players.clear();
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException ex) {
            Logger.getLogger(User.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        this.time = this.setTime;
        this.start = false;
        this.open = true;
    }

    private void begin() {
        this.time = this.setTime;
        this.start = true;
    }

    protected float percentWin(final String njname) {
        if (njname.equals("") ||
                njname.equals("") ||
                njname.equals("") ||
                njname.equals("") ||
                njname.equals("") ||
                njname.equals("")
               ) {
            return util.nextInt(90, 95);
        }

        for (short i = 0; i < this.players.size(); ++i) {
            if (this.players.get(i).name.equals(njname)) {
                return this.players.get(i).joinAmount * 100.0f / this.total;
            }
        }
        return 0.0f;
    }

    @Override
    public void run() {
        while (this.running) {
            try {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                    Logger.getLogger(User.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                }
                if (this.time <= 0 || !this.start) {
                    continue;
                }
                --this.time;
                if (this.time == 0) {
                    this.turned();
                } else {
                    if (this.time >= 10) {
                        continue;
                    }
                    this.open = false;
                }
                continue;
            } catch (Exception e) {
                e.printStackTrace();
            }
            break;
        }
        util.Debug("Close Thread Lucky");
    }

    protected void luckMessage(final User p) throws IOException {
        final Message m = new Message(53);
        m.writer().writeUTF("typemoi");
        m.writer().writeUTF(this.title);
        m.writer().writeShort(this.time);
        m.writer().writeUTF(util.getFormatNumber(this.total) + " " + currency);
        m.writer().writeShort((short) this.percentWin(p.nj.name));
        m.writer().writeUTF((util.parseString("" + this.percentWin(p.nj.name), ".") == null) ? "0" : util.parseString("" + this.percentWin(p.nj.name), "."));
        m.writer().writeShort(this.numPlayers);
        m.writer().writeUTF(this.winnerInfo);
        m.writer().writeByte(this.type);
        m.writer().writeUTF(util.getFormatNumber(this.getJoinAmount(p.nj.name)));
        m.writer().flush();
        p.sendMessage(m);
        m.cleanup();
    }

    public void close() {
        try {
            this.running = false;
            if (this.numPlayers > 0) {
                this.turned();
            }
            this.title = null;
            this.winnerInfo = null;
            this.players = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected static class Players {

        String user;
        String name;
        int joinAmount;
        int id;

        private Players(int id) {
            this.user = null;
            this.name = null;
            this.joinAmount = 0;
            this.id = id;
        }
    }
}
