package server;

import real.ClanManager;
import real.Ninja;
import real.PlayerManager;
import real.User;
import threading.Message;
import threading.Server;

import java.io.IOException;
import real.Item;
import real.ItemData;
import server.RotationLuck.Players;

public class Draw
{
    private static final Server server;
    
    public static void Draw(final User p, final Message m) throws IOException {
        final short menuId = m.reader().readShort();
        final String str = m.reader().readUTF();
        m.cleanup();
//        util.Debug("menuId " + menuId + " str " + str);
        byte b = -1;
        try {
            b = m.reader().readByte();
        }
        catch (IOException ex) {}
        m.cleanup();
        switch (menuId) {
            case 1: {
                if (p.nj.delayVBL < System.currentTimeMillis()) {
                    if (p.nj.quantityItemyTotal(279) <= 0) {
                        break;
                    }
                    final Ninja c = PlayerManager.getInstance().getNinja(str);
                    if (c.getPlace() != null && !c.getPlace().map.isLangCo() && c.getPlace().map.getXHD() == -1) {
                        p.nj.getPlace().leave(p);
                        p.nj.get().x = c.get().x;
                        p.nj.get().y = c.get().y;
                        c.getPlace().Enter(p);
                        p.nj.changeTypePk((short) 0);
                        return;
                    }
                    p.sendYellowMessage("Ví trí người này không thể đi tới");
                    p.nj.delayVBL = System.currentTimeMillis() + 30000L;
                }
                break;
            }
            case 110: {
                if (str.contains("-") || Integer.parseInt(str) < 1) {
                    p.sendYellowMessage("Số lượng không hợp lệ");
                    return;
                }
                if (str != "") {
                    long soluong = Integer.parseInt(str);
                    if ( p.nj.quantityItemyTotal(607) >= 5 * soluong && p.nj.quantityItemyTotal(608) >= 5 * soluong && p.nj.quantityItemyTotal(609) >= 5 * soluong&& p.nj.quantityItemyTotal(610) >= 5 * soluong) {
                        if (p.nj.xu < 30000 * soluong) {
                            p.session.sendMessageLog("Không đủ xu để làm kẹo táo");
                            return;
                        }
                        if (p.nj.getAvailableBag() == 0) {
                            p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                        } else {
                            p.nj.removeItemBags(607, (int) (5 * soluong));
                            p.nj.removeItemBags(608, (int) (5 * soluong));
                            p.nj.removeItemBags(609, (int) (5 * soluong));
                            p.nj.removeItemBags(610, (int) (5 * soluong));
                            p.nj.upXuMessage(-(30000 * soluong));
                            Item it = ItemData.itemDefault(611);
                            it.quantity = (int) (1 * soluong);
                            it.isLock = true;
                            p.nj.addItemBag(true, it);
                        }
                        return;
                    } else {
                        p.nj.getPlace().chatNPC(p, (short) 33, "Hành trang của con không có đủ nguyên liệu");
                    }
                }
                break;
            }
            case 111: {
                if (str.contains("-") || Integer.parseInt(str) < 1) {
                    p.sendYellowMessage("Số lượng không hợp lệ");
                    return;
                }
                if (str != "") {
                    long soluong = Integer.parseInt(str);
                    if ( p.nj.quantityItemyTotal(607) >= 5 * soluong && p.nj.quantityItemyTotal(608) >= 5 * soluong && p.nj.quantityItemyTotal(609) >= 5 * soluong&& p.nj.quantityItemyTotal(610) >= 5 * soluong) {
                        if (p.luong < 30 * soluong) {
                            p.session.sendMessageLog("Không đủ lượng để làm hộp ma quỷ");
                            return;
                        }
                        if (p.nj.getAvailableBag() == 0) {
                            p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                        } else {
                            p.nj.removeItemBags(607, (int) (5 * soluong));
                            p.nj.removeItemBags(608, (int) (5 * soluong));
                            p.nj.removeItemBags(609, (int) (5 * soluong));
                            p.nj.removeItemBags(610, (int) (5 * soluong));
                            p.upluongMessage(-(30 * soluong));
                            Item it = ItemData.itemDefault(612);
                            it.quantity = (int) (1 * soluong);
                            it.isLock = true;
                            p.nj.addItemBag(true, it);
                            p.nj.diemsk+=soluong;
                            p.nj.diemsk1+=soluong;
                        }
                        return;
                    } else {
                        p.nj.getPlace().chatNPC(p, (short) 33, "Hành trang của con không có đủ nguyên liệu");
                    }
                }
                break;
            }
            case 24: {
                final String num = str.replaceAll(" ", "").trim();
                if (num.length() > 10 || !util.checkNumInt(num)) {
                    return;
                }
                p.exchangeLuongXu(Long.parseLong(num));
                break;
            }
            case 25: {
                final String num = str.replaceAll(" ", "").trim();
                if (num.length() > 10 || !util.checkNumInt(num)) {
                    return;
                }
                p.exchangeLuongYen(Long.parseLong(num));
                break;
            }
            case 46:
                p.giftcode = str;
                p.giftcode2();
                break;
            case 47: {
                p.Giftcode(str);
                break;
            }
            case 50: {
                ClanManager.createClan(p, str);
                break;
            }
            case 51: {
                p.passnew = "";
                p.passold = str;
                p.changePassword();
                Draw.server.menu.sendWrite(p, (short)52, "Nhập mật khẩu mới");
                break;
            }
            case 52: {
                p.passnew = str;
                p.changePassword();
                break;
            }
            
//            case 53:
//                p.nameUS = str;
//                Ninja n = PlayerManager.getInstance().getNinja(str);
//                if (n != null) {
//                    server.menu.sendWrite(p, (short) 54, "Nhập ID vật phẩm");
//                } else {
//                    p.sendYellowMessage("Nhập sai tên");
//                }
//                break;
//            case 54:
//                Ninja s = PlayerManager.getInstance().getNinja(p.nameUS);
//                String it = str.replaceAll(" ", "").trim();
//                int item = Integer.parseInt(it);
//                p.sendItem(p, s.p, item);
//                break;
            case 100: {
                final String num = str.replaceAll(" ", "").trim();
                if (num.length() > 10 || !util.checkNumInt(num) || b < 0 || b >= Draw.server.manager.rotationluck.length) {
                    return;
                }
                final int xujoin = Integer.parseInt(num);
                for (Players player : Draw.server.manager.rotationluck[0].players) {
                    if (player.user.equals(p.username) || player.name.equals(p.nj.name)) {
                        p.session.sendMessageLog("Không thể tham gia 2 vong xoay cùng 1 lúc");
                        return;
                    }
                }
                for (Players player : Draw.server.manager.rotationluck[1].players) {
                    if (player.user.equals(p.username) || player.name.equals(p.nj.name)) {
                        p.session.sendMessageLog("Không thể tham gia 2 vong xoay cùng 1 lúc");
                        return;
                    }
                }
                Draw.server.manager.rotationluck[b].joinLuck(p, xujoin);
                break;
            }
            case 101: {
                if (b < 0 || b >= Draw.server.manager.rotationluck.length) {
                    return;
                }
                Draw.server.manager.rotationluck[b].luckMessage(p);
                break;
            }
            case 102: {
                p.typemenu = 92;
                MenuController.doMenuArray(p, new String[] { "Vòng xoay vip", "Vòng xoay thường" });
                break;
            }
//            case 103: {
//                final String num = str.replaceAll(" ", "").trim();
//                if (num.length() > 10 || !util.checkNumInt(num)) {
//                    return;
//                }
//                p.chanXu(Long.parseLong(num));
//                break;
//            }
//            case 104: {
//                final String num = str.replaceAll(" ", "").trim();
//                if (num.length() > 10 || !util.checkNumInt(num)) {
//                    return;
//                }
//                p.leXu(Long.parseLong(num));
//                break;
//            }
//            case 105: {
//                final String num = str.replaceAll(" ", "").trim();
//                if (num.length() > 10 || !util.checkNumInt(num)) {
//                    return;
//                }
//                p.chanLuong(Long.parseLong(num));
//                break;
//            }
//            case 106: {
//                final String num = str.replaceAll(" ", "").trim();
//                if (num.length() > 10 || !util.checkNumInt(num)) {
//                    return;
//                }
//                p.leLuong(Long.parseLong(num));
//                break;
//            }
//            case 9998: {
//                try {
//                    if(!util.checkNumInt(str) || str.equals("")) {
//                        p.session.sendMessageLog("Giá trị nhập vào không hợp lệ");
//                        return;
//                    }
//                    String check = str.replaceAll(" ", "").trim();
//                    int minues = Integer.parseInt(check);
//                    if( minues < 0 || minues > 10) {
//                        p.session.sendMessageLog("Giá trị nhập vào từ 0 -> 10 phút");
//                        return;
//                    }
//                    p.sendYellowMessage("Đã kích hoạt bảo trì Server sau " + minues + " phút.");
//                    for (int i = 0; i < minues; i++) {
//                        Manager.serverChat("Thông báo", "Máy chủ sẽ tiến hành bảo trì sau " + minues - i + " phút nữa. Vui lòng thoát game để tránh mất dữ liệu.");
//                        Thread.sleep(60000);
//                    }
//                    PlayerManager.getInstance().Clear();
//                    server.stop();
//                    break;
//                }
//                catch (InterruptedException ex) {}
//            }
        }
    }
    
    static {
        server = Server.getInstance();
    }
}
