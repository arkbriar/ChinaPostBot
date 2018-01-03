import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import model.Post;
import model.PostRoute;

/**
 * Created by Shunjie Ding on 03/01/2018.
 */
public class TestParse {
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Test
    public void testPostJson() throws IOException, ParseException {
        String value =
            "{\"rdata\":[{\"D_SJSJ\":\"2017-10-16 17:52:41\",\"FROM_FLAG\":\"0\",\"GJ_FLAG\":\"GN\",\"V_YJHM\":\"9891337437041\",\"V_JDJMC\":\"湖北省黄州区\",\"V_ZT\":\"已签收,超市 代收\",\"N_XH\":1,\"V_HJDM\":\"TT\",\"V_JDJDM\":\"43800000\",\"N_TDBZ\":1}]}";
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(dateFormat);
        JsonNode root = objectMapper.readTree(value);
        List<Post> posts = Arrays.asList(objectMapper.treeToValue(root.get("rdata"), Post[].class));

        Assert.assertEquals(posts.size(), 1);

        Post post = posts.get(0);
        Assert.assertEquals(post.getId(), "9891337437041");
        Assert.assertEquals(post.getNowLocation(), "湖北省黄州区");
        Assert.assertEquals(post.getReceiveTime(), dateFormat.parse("2017-10-16 17:52:41"));
        Assert.assertEquals(post.getStatus(), "已签收,超市 代收");
    }

    @Test
    public void testPostRouteJson() throws IOException {
        String value =
            "{\"rdata\":[{\"V_TTBZ\":\"203\",\"D_SJSJ\":\"2017-10-13 13:48:58\",\"V_CXCLJBH\":\"21556400\",\"V_ZT\":\"【\\u003ca\\u003e全国中心中国邮政集团公司江苏省常熟市函件业务局\\u003c/a\\u003e】已收寄\",\"N_XH\":2,\"V_ZTXX\":\"0\",\"V_HJDM\":\"SJ\"},{\"V_TTBZ\":\"0\",\"D_SJSJ\":\"2017-10-13 13:53:16\",\"V_CXCLJBH\":\"21556400\",\"V_ZT\":\"【\\u003ca\\u003e江苏省中国邮政集团公司江苏省常熟市函件分局\\u003c/a\\u003e】已收寄\",\"N_XH\":3,\"V_ZTXX\":\"0\",\"V_HJDM\":\"SJ\"},{\"V_TTBZ\":\"0\",\"D_SJSJ\":\"2017-10-13 18:33:00\",\"V_CXCLJBH\":\"0\",\"V_ZT\":\"离开【中国邮政集团公司江苏省常熟市函件分局】，下一站【苏州】\",\"N_XH\":1,\"V_ZTXX\":\"0\",\"V_HJDM\":\"FY\"},{\"V_TTBZ\":\"0\",\"D_SJSJ\":\"2017-10-13 18:33:00\",\"V_CXCLJBH\":\"0\",\"V_ZT\":\"离开【中国邮政集团公司江苏省常熟市函件分局】，下一站【苏州】\",\"N_XH\":9,\"V_ZTXX\":\"0\",\"V_HJDM\":\"FY\"},{\"V_TTBZ\":\"303\",\"D_SJSJ\":\"2017-10-13 20:38:38\",\"V_CXCLJBH\":\"0\",\"V_ZT\":\"离开【常熟函件】\",\"N_XH\":14,\"V_ZTXX\":\"0\",\"V_HJDM\":\"FY\"},{\"V_TTBZ\":\"0\",\"D_SJSJ\":\"2017-10-14 14:21:04\",\"V_CXCLJBH\":\"0\",\"V_ZT\":\"到达【\\u003ca\\u003e苏州中心\\u003c/a\\u003e】\",\"N_XH\":4,\"V_ZTXX\":\"0\",\"V_HJDM\":\"DD\"},{\"V_TTBZ\":\"0\",\"D_SJSJ\":\"2017-10-14 20:41:21\",\"V_CXCLJBH\":\"0\",\"V_ZT\":\"离开【苏州中心】，下一站【武汉中心】\",\"N_XH\":8,\"V_ZTXX\":\"0\",\"V_HJDM\":\"FY\"},{\"V_TTBZ\":\"0\",\"D_SJSJ\":\"2017-10-15 08:32:41\",\"V_CXCLJBH\":\"0\",\"V_ZT\":\"到达【\\u003ca\\u003e武汉中心\\u003c/a\\u003e】\",\"N_XH\":5,\"V_ZTXX\":\"0\",\"V_HJDM\":\"DD\"},{\"V_TTBZ\":\"0\",\"D_SJSJ\":\"2017-10-15 15:31:08\",\"V_CXCLJBH\":\"0\",\"V_ZT\":\"离开【武汉中心】，下一站【黄冈转运】\",\"N_XH\":10,\"V_ZTXX\":\"0\",\"V_HJDM\":\"FY\"},{\"V_TTBZ\":\"0\",\"D_SJSJ\":\"2017-10-15 17:19:00\",\"V_CXCLJBH\":\"0\",\"V_ZT\":\"到达【\\u003ca\\u003e黄冈转运\\u003c/a\\u003e】\",\"N_XH\":7,\"V_ZTXX\":\"0\",\"V_HJDM\":\"DD\"},{\"V_TTBZ\":\"0\",\"D_SJSJ\":\"2017-10-16 07:09:18\",\"V_CXCLJBH\":\"0\",\"V_ZT\":\"离开【黄冈转运】，下一站【HGKF】\",\"N_XH\":11,\"V_ZTXX\":\"0\",\"V_HJDM\":\"FY\"},{\"V_TTBZ\":\"0\",\"D_SJSJ\":\"2017-10-16 08:59:35\",\"V_CXCLJBH\":\"43800011\",\"V_ZT\":\"【\\u003ca\\u003e开发区投递站\\u003c/a\\u003e】接收\",\"N_XH\":6,\"V_ZTXX\":\"0\",\"V_HJDM\":\"DD\"},{\"V_TTBZ\":\"0\",\"D_SJSJ\":\"2017-10-16 09:00:18\",\"V_CXCLJBH\":\"43800011\",\"V_ZT\":\"【\\u003ca\\u003e开发区投递站\\u003c/a\\u003e】正在投递,投递员：吴新海 18007250528\",\"N_XH\":13,\"V_ZTXX\":\"0\",\"V_HJDM\":\"APTD\"},{\"V_TTBZ\":\"他人收\",\"D_SJSJ\":\"2017-10-16 17:52:41\",\"V_CXCLJBH\":\"43800011\",\"V_ZT\":\"已签收,超市 代收,投递员：吴新海 18007250528 \",\"N_XH\":12,\"V_ZTXX\":\"0\",\"V_HJDM\":\"TT\"}]}";
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(dateFormat);
        JsonNode root = objectMapper.readTree(value);
        List<PostRoute> routes =
            Arrays.asList(objectMapper.treeToValue(root.get("rdata"), PostRoute[].class));

        Assert.assertEquals(routes.size(), 14);
        Assert.assertEquals(routes.get(0).getType(), PostRoute.RouteType.ACCEPT);
        Assert.assertEquals(routes.get(routes.size() - 2).getType(), PostRoute.RouteType.DELIVERY);
        Assert.assertEquals(routes.get(routes.size() - 1).getType(), PostRoute.RouteType.RECEIPT);
    }
}
