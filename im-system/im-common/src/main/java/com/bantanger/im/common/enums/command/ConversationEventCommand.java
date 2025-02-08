package com.bantanger.im.common.enums.command;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 17:11
 */
public enum ConversationEventCommand implements Command {

    //删除会话 5000 -> 0x1388
    CONVERSATION_DELETE(0x1388),

    //更新会话 5001 -> 0x1389
    CONVERSATION_UPDATE(0x1389),

    ;

    private Integer command;

    ConversationEventCommand(int command){
        this.command=command;
    }

    @Override
    public Integer getCommand() {
        return command;
    }

}
