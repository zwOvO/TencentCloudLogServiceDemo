package io.github.zwovo;

import io.github.zwovo.proto.Cls;
import io.github.zwovo.service.LogService;

import java.io.UnsupportedEncodingException;

public class clsUploadLog {

    public static void main(String[] args) {

        LogService logService = new LogService();
        Cls.LogGroup.Builder logGroupBuild = Cls.LogGroup.newBuilder();
        for (int i = 0; i < 5; i++) {
            Cls.Log log = Cls.Log.newBuilder()
                    .addContents(Cls.Log.Content.newBuilder().setKey("key" + i).setValue("value" + i).build())
                    .setTime(System.currentTimeMillis())
                    .build();
            logGroupBuild.addLogs(log);
        }
        Cls.LogGroup logGroup = logGroupBuild.build();
        Cls.LogGroupList logGroupList = Cls.LogGroupList.newBuilder().addLogGroupList(logGroup).build();
        String response = null;
        try {
            response = logService.insert(logGroupList);
            System.out.println(response);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
