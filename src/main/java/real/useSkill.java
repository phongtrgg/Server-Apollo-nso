package real;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import patch.MessageSubCommand;
import server.util;
import threading.Message;

public class useSkill {

    public static void useSkill(User p, Message m) {
        try {
            if (m.reader().available() <= 0) {
                return;
            }
            short idSkill = m.reader().readShort();
            m.cleanup();
            Skill skill = p.nj.get().getSkill(idSkill);
            if (skill != null && System.currentTimeMillis() > p.nj.get().CSkilldelay) {
                SkillData data = SkillData.Templates(idSkill);
                if (data.type != 0) {
                    p.nj.get().CSkilldelay = System.currentTimeMillis() + 500L;
                    //System.out.println("data type==============="+data.type);
                    switch (data.type) {

                        case 2: {
                            useSkillBuff(p, idSkill);
                            break;
                        }
                        case 3: {
                            useSkillMagic(p, idSkill);
                            break;
                        }
                        case 4: {
                            p.nj.get().setCSkill(49);
                            break;
                        }
                        default: {

                            p.nj.get().setCSkill(idSkill);
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            //Util.Debug("Error useSkill.java(38) : " + e.toString());
        } catch (SQLException ex) {
            Logger.getLogger(useSkill.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (m != null) {
                m.cleanup();
            }
        }
    }

    public static void useSkillBot(User p, short idSkill) {
        try {
            Skill skill = p.nj.get().getSkill(idSkill);
            if (skill != null) {
                useSkillBuff(p, idSkill);
            }
        } catch (Exception e) {

        }
    }

    private static void useSkillMagic(User p, int skilltemp) {
        Skill skill = p.nj.get().getSkill(skilltemp);
        SkillTemplates temp = SkillData.Templates(skill.id, skill.point);
        if (p.nj.get().mp < temp.manaUse) {
            try {
                p.getMp();
            } catch (IOException ex) {
                Logger.getLogger(useSkill.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
        }
        if (skill.coolDown > System.currentTimeMillis()) {
            return;
        }
        p.nj.get().upMP(-temp.manaUse);
        skill.coolDown = System.currentTimeMillis() + temp.coolDown;
        int param = 0;
        switch (skilltemp) {
            //skill 25 kiếm
            case 4: {
                //p.nj.isSkill25Kiem = true;
                break;
            }
            //skill 25 đao
            case 40: {
                //p.c.isSkill25Dao = true;
                break;
            }
        }
    }

    private static void useSkillBuff(User p, int skilltemp) throws SQLException {
        Skill skill = p.nj.get().getSkill(skilltemp);
        SkillTemplates temp = SkillData.Templates(skill.id, skill.point);
        if (p.nj.get().mp < temp.manaUse) {
            try {
                p.getMp();
                return;
            } catch (IOException ex) {
                Logger.getLogger(useSkill.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (skill.coolDown > System.currentTimeMillis()) {
            return;
        }
        p.nj.get().upMP(-temp.manaUse);
        skill.coolDown = System.currentTimeMillis() + temp.coolDown;
        int param = 0;
        switch (skilltemp) {
            case 6: {
                p.setEffect(15, 0, p.nj.get().getPramSkill(53) * 1000, 0);
                break;
            }
            case 13: {
                p.setEffect(9, 0, 30000, p.nj.get().getPramSkill(51));
                break;
            }
            case 15: {
                p.setEffect(16, 0, 5000, p.nj.get().getPramSkill(52));
                break;
            }
            case 96: {
                p.setEffect(46, 0, p.nj.get().getPramSkill(53) * 1000, 0);
                break;
            }
            //Gốc cây
            case 22: {
                int per = util.nextInt(0, 1);
                p.nj.getPlace().addBuNhin(new BuNhin(p.nj.name, p.nj.x, p.nj.y, temp.options.get(0).param * 1000, p.nj.id, p.nj.hp));

                break;
            }
            case 31: {
                p.setEffect(10, 0, 90000, p.nj.get().getPramSkill(30));
                break;
            }
            case 33: {
                p.setEffect(17, 0, 5000, p.nj.get().getPramSkill(56));
                break;
            }
            case 47: {
                param = p.nj.get().getPramSkill(27);
                param += param * p.nj.get().getPramSkill(66) / 100;
                p.setEffect(8, 0, 5000, param);
                if (p.nj.get().party != null) {
                    for (User p2 : p.nj.getPlace().getUsers()) {
                        if (p2.nj.id != p.nj.id) {
                            final Ninja n = p2.nj;
                            if (n.party == p.nj.get().party && Math.abs(p.nj.get().x - n.x) <= temp.dx && Math.abs(p.nj.get().y - n.y) <= temp.dy) {
                                n.p.setEffect(8, 0, 5000, p.nj.get().getPramSkill(43) + p.nj.get().getPramSkill(43) * p.nj.get().getPramSkill(66) / 100);
                            }
                        }
                    }
                    break;
                }
                break;
            }
            case 51: {
                param = p.nj.get().getPramSkill(45);
                param += param * p.nj.get().getPramSkill(66) / 100;
                p.setEffect(19, 0, 90000, param);
                if (p.nj.get().party != null) {
                    for (User p2 : p.nj.getPlace().getUsers()) {
                        if (p2.nj.id != p.nj.id) {
                            final Ninja n = p2.nj;
                            if (n.party == p.nj.get().party && Math.abs(p.nj.x - n.x) <= temp.dx && Math.abs(p.nj.y - n.y) <= temp.dy) {
                                n.p.setEffect(19, 0, 90000, param);
                            }
                        }
                    }
                    break;
                }
                break;
            }
            case 52: {
                p.setEffect(20, 0, p.nj.get().getPramSkill(54) * 1000, p.nj.get().getPramSkill(66));
                if (p.nj.get().party != null) {
                    for (User p2 : p.nj.getPlace().getUsers()) {
                        if (p2.nj.id != p.nj.id) {
                            final Ninja n = p2.nj;
                            if (n.party == p.nj.get().party && Math.abs(p.nj.get().x - n.x) <= temp.dx && Math.abs(p.nj.get().y - n.y) <= temp.dy) {
                                n.p.setEffect(20, 0, p.nj.get().getPramSkill(54) * 1000, p.nj.get().getPramSkill(66));
                            }
                        }
                    }
                    break;
                }
                break;
            }
            case 97: {
                param = p.nj.get().getPramSkill(27);
                p.setEffect(47, 0, 5000, param);
                break;
            }
            case 58: {
                p.setEffect(11, 0, p.nj.get().getPramSkill(64), 20000);
                break;
            }
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 99: {
//                if (p.nj.timeRemoveCloneSave > System.currentTimeMillis()) {
//                    p.c.clone.islive = true;
//                    p.c.clone = CloneCharacter.getClone(p.c);
//                    p.c.clone.open(p.c.timeRemoveCloneSave, (int) p.c.getPramSkill(71));
//                    return;
//                }
//                if (p.c.timeRemoveClone > System.currentTimeMillis()) {
//                    p.c.clone.islive = true;
//                    p.c.clone = CloneCharacter.getClone(p.c);
//                    p.c.clone.open(p.c.timeRemoveClone, (int) p.c.getPramSkill(71));
//                    return;
//                }
//
//                for (byte j = 0; j < p.c.get().ItemBody.length; j++) {
//                    Item item = p.c.get().ItemBody[j];
//                    if (item != null && item.id == 943) {
//                        if (p.c.timeRemoveClone <= System.currentTimeMillis()) {
//                            p.sendAddchatYellow("Bạn đang Hợp Thể");
//                            return;
//                        }
//                    }
//                }
//
//                if (p.c.timeRemoveClone <= System.currentTimeMillis() && p.c.quantityItemyTotal(545) <= 0) {
//                    p.sendAddchatYellow("Không có đủ " + ItemTemplate.ItemTemplateId(545).name);
//                    return;
//                }
//                p.c.clone.islive = true;
//                p.c.clone = CloneCharacter.getClone(p.c);
//                p.c.clone.open(System.currentTimeMillis() + 60000 * p.c.getPramSkill(68), (int) p.c.getPramSkill(71));
//                if (p.c.quantityItemyTotal(545) > 0) {
//                    p.c.removeItemBags(545, 1);
//                    return;
//                }
                if (p.nj.timeRemoveClone <= System.currentTimeMillis() && p.nj.quantityItemyTotal(545) <= 0) {
                    p.sendYellowMessage("Không có đủ " + ItemData.ItemDataId(545).name);
                    break;
                }
                p.nj.clone.open(System.currentTimeMillis() + 60000 * p.nj.getPramSkill(68), p.nj.getPramSkill(71));
                if (p.nj.quantityItemyTotal(545) > 0) {
                    p.nj.removeItemBags(545, 1);
                    break;
                }
                break;
            }

        }
    }

    public static void useSkillCloneBuff(Body body, int skilltemp) {
        Skill skill = body.getSkill(skilltemp);
        if (skill != null) {
            SkillTemplates temp = SkillData.Templates(skill.id, skill.point);
            if (skill.coolDown <= System.currentTimeMillis()) {
                skill.coolDown = System.currentTimeMillis() + (long) temp.coolDown;
                User p = body.c.p;
                int param;
                switch (skilltemp) {
                    case 6: {
                        p.setEffect(15, 0, body.getPramSkill(53) * 1000, 0);
                        break;
                    }
                    case 13: {
                        p.setEffect(9, 0, 30000, body.getPramSkill(51));
                        break;
                    }
                    case 15: {
                        p.setEffect(16, 0, 5000, body.getPramSkill(52));
                        break;
                    }
                    case 31: {
                        p.setEffect(10, 0, 90000, body.getPramSkill(30));
                        break;
                    }
                    case 33: {
                        p.setEffect(17, 0, 5000, body.getPramSkill(56));
                        break;
                    }
                    case 47: {
                        param = body.getPramSkill(27);
                        param += param * body.getPramSkill(66) / 100;
                        p.setEffect(8, 0, 5000, param);
                        if (body.party != null) {
                            for (User p2 : p.nj.getPlace().getUsers()) {
                                if (p2.nj.id != p.nj.id) {
                                    final Ninja n = p2.nj;
                                    if (n.party == body.party && Math.abs(body.x - n.x) <= temp.dx && Math.abs(body.y - n.y) <= temp.dy) {
                                        n.p.setEffect(8, 0, 5000, body.getPramSkill(43) + body.getPramSkill(43) * body.getPramSkill(66) / 100);
                                    }
                                }
                            }
                            break;
                        }
                        break;
                    }
                    case 51:
                        param = body.getPramSkill(45);
                        param += body.getPramSkill(66);
                        p.setEffect(19, 0, 90000, param);
                        break;
                    case 52:
                        param = body.getPramSkill(40) * 1000;
                        param += param * body.getPramSkill(66) / 100;
                        p.setEffect(20, 0, param, 0);
                        break;
                }
            }
        }
    }

    public static void buffLive(User p, Message m) {
        try {

            final int idP = m.reader().readInt();
            final Ninja nj = p.nj.getPlace().getNinja(idP);
            m.cleanup();
            final Skill skill = p.nj.get().getSkill(p.nj.get().getCSkill());
            if (nj != null && nj.isDie && skill.id == 49) {
                final SkillTemplates temp = SkillData.Templates(skill.id, skill.point);
                if (p.nj.get().mp < temp.manaUse) {
                    MessageSubCommand.sendMP(p.nj);
                    return;
                }
                if (skill.coolDown > System.currentTimeMillis() || Math.abs(p.nj.get().x - nj.x) > temp.dx || Math.abs(p.nj.get().y - nj.y) > temp.dy) {
                    return;
                }
                p.nj.get().upMP(-temp.manaUse);
                skill.coolDown = System.currentTimeMillis() + temp.coolDown;
                try {
                    nj.p.liveFromDead();
                } catch (IOException ex) {
                    Logger.getLogger(useSkill.class.getName()).log(Level.SEVERE, null, ex);
                }
                nj.p.setEffect(11, 0, 5000, p.nj.get().getPramSkill(28));
            }
        } catch (IOException ex) {
            Logger.getLogger(useSkill.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
