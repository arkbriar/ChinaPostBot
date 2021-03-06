package task;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.concurrent.Task;
import model.Post;
import model.PostRoute;

/**
 * Created by Shunjie Ding on 31/12/2017.
 */
public class QueryTask extends Task<File> {
    public static final String COOKIE_FILE = "cookies.ser";
    private static final int POST_QUERY_LIMIT = 40;
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_CONCURRENT_THREADS = 25;
    private static final String[] COLUMNS = {"售后人员", "查询日期", "收寄日期", "客户", "运单号",
        "收件人", "联系方式", "地址", "投诉类别", "物流", "答复", "完成状态"};
    private static PoolingHttpClientConnectionManager httpClientConnectionManager =
        new PoolingHttpClientConnectionManager();
    private static CookieStore cookieStore = new BasicCookieStore();
    private static HttpClient httpClient = HttpClientBuilder.create()
                                               .setDefaultCookieStore(cookieStore)
                                               .setConnectionManager(httpClientConnectionManager)
                                               .build();

    static {
        httpClientConnectionManager.setDefaultMaxPerRoute(MAX_CONCURRENT_THREADS * 5);
        httpClientConnectionManager.setMaxTotal(MAX_CONCURRENT_THREADS * 10);
    }

    private final Logger logger = Logger.getLogger(QueryTask.class.getName());
    private final String name;
    private final String filePath;
    private Font cellFont;
    private Font cellFontBold;
    private CellStyle cellStyleCommon;
    private CellStyle cellStyleDate;
    private CellStyle cellStyleTitle;
    private int taskMax;

    public QueryTask(String name, String filePath) {
        this.name = name;
        this.filePath = filePath;
    }

    private static String getCookieString() {
        return cookieStore.getCookies()
            .stream()
            .map(c -> String.format("%s=%s", c.getName(), c.getValue()))
            .collect(Collectors.joining("; "));
    }

    private static HttpResponse doGet(String url) throws IOException {
        HttpGet httpGet = new HttpGet(url);

        // Set headers
        httpGet.setHeader("Accept-Encoding", "gzip, deflate");
        httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7,en-US;q=0.6");
        httpGet.setHeader("User-Agent",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Safari/537.36");
        httpGet.setHeader("Upgrade-Insecure-Requests", "1");
        httpGet.setHeader("Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        httpGet.setHeader("Cache-Control", "max-age=0");
        httpGet.setHeader("Connection", "keep-alive");

        return httpClient.execute(httpGet);
    }

    public static void loadCookiesFromFile(File file) throws IOException, ClassNotFoundException {
        try (ObjectInput input = new ObjectInputStream(new FileInputStream(file))) {
            CookieStore cookieStoreLoaded = (CookieStore) input.readObject();
            if (!cookieStoreLoaded.equals(cookieStore)) {
                cookieStore = cookieStoreLoaded;
                httpClient = HttpClientBuilder.create()
                                 .setDefaultCookieStore(cookieStore)
                                 .setConnectionManager(httpClientConnectionManager)
                                 .build();
            }
        }
    }

    public static void saveCookiesToFile(File file) throws IOException {
        try (ObjectOutput output = new ObjectOutputStream(new FileOutputStream(file))) {
            output.writeObject(cookieStore);
        }
    }

    public static HttpResponse visitMainPage() throws IOException {
        return doGet("http://yjcx.chinapost.com.cn/zdxt/yjcx");
    }

    private static HttpResponse doPost(String url, List<NameValuePair> params) throws IOException {
        HttpPost httpPost = new HttpPost(url);

        // Set headers
        httpPost.setHeader("Origin", "http://yjcx.chinapost.com.cn");
        httpPost.setHeader("Accept-Encoding", "gzip, deflate");
        httpPost.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7,en-US;q=0.6");
        httpPost.setHeader("User-Agent",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Safari/537.36");
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        httpPost.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        httpPost.setHeader("Referer",
            "http://yjcx.chinapost.com.cn/zdxt/jsp/zhdd/gjyjgzcx/gjyjqcgzcx/gjyjqcgzcx_new.jsp");
        httpPost.setHeader("X-Requested-With", "XMLHttpRequest");
        httpPost.setHeader("Connection", "keep-alive");

        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        return httpClient.execute(httpPost);
    }

    private List<String> loadFromFile(String filePath) throws IOException, InvalidFormatException {
        Workbook workbook = WorkbookFactory.create(new File(filePath));
        Sheet sheet = workbook.getSheetAt(0);
        List<String> ids = new ArrayList<>(sheet.getPhysicalNumberOfRows());

        final Pattern pattern = Pattern.compile("\\d{13}");
        for (int i = sheet.getFirstRowNum(); i < sheet.getLastRowNum() + 1; ++i) {
            if (sheet.getRow(i) == null) {
                continue;
            }
            Row row = sheet.getRow(i);
            if (row.getCell(0) == null) {
                continue;
            }

            String id = row.getCell(0).getStringCellValue();
            // ignore those null, empty and not match
            if (id == null || id.isEmpty() || !pattern.matcher(id).matches()) {
                continue;
            }
            ids.add(id);
        }
        return ids;
    }

    private List<NameValuePair> getPostQueryParams(List<String> ids) {
        String idList =
            ids.stream().map(id -> String.format("'%s'", id)).collect(Collectors.joining(","));
        List<NameValuePair> params = new ArrayList<>(4);
        params.add(new BasicNameValuePair("vYjhmLst", idList));
        params.add(new BasicNameValuePair("vDzyhbh", ""));
        params.add(new BasicNameValuePair("checkYjh", ""));
        params.add(new BasicNameValuePair("cgsyj", "null"));
        return params;
    }

    private ObjectMapper newObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(dateFormat);
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
        return mapper;
    }

    private List<Post> queryPosts(List<String> ids) {
        List<NameValuePair> params = getPostQueryParams(ids);

        for (int i = 0; i < 2; ++i) {
            try {
                HttpResponse response = doPost(
                    "http://yjcx.chinapost.com.cn/zdxt/gjyjqcgzcx/gjyjqcgzcx_NewgjyjqcgzcxDqztQueryPage.action",
                    params);
                // If response isn't OK, do it once more.
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    continue;
                }

                HttpEntity httpEntity = response.getEntity();
                if (httpEntity == null) {
                    continue;
                }

                ObjectMapper objectMapper = newObjectMapper();
                JsonNode root = objectMapper.readTree(httpEntity.getContent());
                List<Post> posts =
                    Arrays.asList(objectMapper.treeToValue(root.get("rdata"), Post[].class));

                assert posts.size() == ids.size();

                // Return post list on success
                return posts;
            } catch (IOException e) {
                e.printStackTrace();
                // Try once more
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }

        // Return empty list
        return new ArrayList<>();
    }

    private List<PostRoute> queryPostRoutes(Post post) {
        List<NameValuePair> params = post.paramsForQuery();

        for (int i = 0; i < 2; ++i) {
            try {
                HttpResponse response = doPost(
                    "http://yjcx.chinapost.com.cn/zdxt/gjyjqcgzcx/gjyjqcgzcx_NewgjyjqcgzcxLzxxQueryPage.action",
                    params);
                // If response isn't OK, do it once more.
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    continue;
                }

                HttpEntity httpEntity = response.getEntity();
                if (httpEntity == null) {
                    continue;
                }

                ObjectMapper objectMapper = newObjectMapper();
                JsonNode root = objectMapper.readTree(httpEntity.getContent());

                // Return route list on success
                return Arrays.asList(
                    objectMapper.treeToValue(root.get("rdata"), PostRoute[].class));
            } catch (IOException e) {
                e.printStackTrace();
                // Try once more
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }

        // Return empty list
        return new ArrayList<>();
    }

    private String outputPath(String filePath) {
        File file = new File(filePath);
        String filename = file.getName();
        int idx = filename.lastIndexOf('.');
        assert idx != -1;

        return Paths.get(file.getParent(), filename.substring(0, idx) + "_结果.xlsx").toString();
    }

    private String getFontName() {
        String osName = System.getProperty("os.name");
        switch (osName) {
            case "Windows":
                return "楷体";
            default:
                return "KaiTi";
        }
    }

    private Font getDefaultFont(Workbook workbook) {
        if (cellFont == null) {
            cellFont = workbook.createFont();
            cellFont.setFontName(getFontName());
            cellFont.setFontHeightInPoints((short) 10);
        }
        return cellFont;
    }

    private Font getDefaultBoldFont(Workbook workbook) {
        if (cellFontBold == null) {
            cellFontBold = workbook.createFont();
            cellFontBold.setFontName(getFontName());
            cellFontBold.setBold(true);
            cellFontBold.setFontHeightInPoints((short) 10);
        }
        return cellFontBold;
    }

    private CellStyle getCellStyleDate(Workbook workbook) {
        if (cellStyleDate == null) {
            cellStyleDate = workbook.createCellStyle();
            cellStyleDate.setFont(getDefaultFont(workbook));
            cellStyleDate.setDataFormat(
                workbook.getCreationHelper().createDataFormat().getFormat("mm月dd日"));
        }
        return cellStyleDate;
    }

    public CellStyle getCellStyleTitle(Workbook workbook) {
        if (cellStyleTitle == null) {
            cellStyleTitle = workbook.createCellStyle();
            cellStyleTitle.setFont(getDefaultBoldFont(workbook));
        }
        return cellStyleTitle;
    }

    private CellStyle getCommonStyle(Workbook workbook) {
        if (cellStyleCommon == null) {
            cellStyleCommon = workbook.createCellStyle();
            cellStyleCommon.setFont(getDefaultFont(workbook));
        }
        return cellStyleCommon;
    }

    private Workbook newWorkbook() {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        SXSSFSheet mainSheet = workbook.createSheet("主动");
        mainSheet.trackAllColumnsForAutoSizing();

        Row titleRow = mainSheet.createRow(0);

        for (int i = 0; i < COLUMNS.length; ++i) {
            titleRow.createCell(i).setCellValue(COLUMNS[i]);
        }

        // Format title
        for (int i = 0; i < COLUMNS.length; ++i) {
            titleRow.getCell(i).setCellStyle(getCellStyleTitle(workbook));
        }

        return workbook;
    }

    private String getLastArrival(List<PostRoute> postRoutes) {
        for (int i = postRoutes.size() - 1; i >= 0; --i) {
            // There are cases that have no ARRIVAL, so just using regex.
            String status = postRoutes.get(i).getStatus();
            Pattern pattern = Pattern.compile(".*【<a>(\\S+)</a>】.*");
            Matcher matcher = pattern.matcher(status);

            if (matcher.find()) {
                return String.format("【%s】", matcher.group(1));
            }
        }

        return "【未知】";
    }

    private String getPersonSignedWithRegex(String p, String status) {
        Pattern pattern = Pattern.compile(p);
        Matcher matcher = pattern.matcher(status);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String getPersonSigned(List<PostRoute> postRoutes) {
        PostRoute last = postRoutes.get(postRoutes.size() - 1);
        assert last.getType() == PostRoute.RouteType.RECEIPT;

        String name;
        // Case 1
        name = getPersonSignedWithRegex("已签收,([^ ;,，；]+).*代收.*", last.getStatus());
        if (!name.isEmpty()) {
            return name;
        }

        // Case 2
        name = getPersonSignedWithRegex("已签收,代投点：([^ ;,，；]+).*", last.getStatus());
        if (!name.isEmpty()) {
            return name;
        }

        name = getPersonSignedWithRegex("已签收,包裹柜：([^ ;,，；]+)收.*", last.getStatus());
        if (!name.isEmpty()) {
            return name;
        }

        return name;
    }

    private Calendar getCalendar() {
        // FIXME Add some strategies, such uniformly select dates of this month.
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private Calendar getCalendar(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private String getSimpleDateString(Calendar calendar) {
        return (calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.DATE);
    }

    private String getComplaintType(boolean isDelivered) {
        if (isDelivered) {
            // successfully delivered
            return "信息未更新，实际已妥投";
        } else {
            return "其他";
        }
    }

    private String getPostStatus(Post post, List<PostRoute> postRoutes) {
        if (isDelivered(postRoutes)) {
            String signedWho = getPersonSigned(postRoutes);
            String deliveryInfo = getLastArrival(postRoutes) + " 已妥投";
            if (!signedWho.isEmpty()) {
                deliveryInfo += "，签收人: " + signedWho;
            }
            if (post.getStatus().contains("704")) {
                deliveryInfo += " 704【转】";
            }
            return deliveryInfo;
        } else {
            return post.getStatus();
        }
    }

    private String getReply(boolean isDelivered, Calendar calendar) {
        if (isDelivered) {
            return getSimpleDateString(calendar) + "，已签收";
        } else {
            return "";
        }
    }

    private String getStatus(boolean isDelivered) {
        return isDelivered ? "已妥投" : "待处理";
    }

    private boolean isDelivered(List<PostRoute> postRoutes) {
        return !postRoutes.isEmpty()
            && postRoutes.get(postRoutes.size() - 1).getType() == PostRoute.RouteType.RECEIPT;
    }

    private void writeToTable(Workbook workbook, Post post, List<PostRoute> postRoutes) {
        if (post == null || postRoutes == null || postRoutes.isEmpty()) {
            // ignore invalid cases
            return;
        }

        final boolean isDelivered = isDelivered(postRoutes);
        Sheet sheet = workbook.getSheetAt(0);
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);

        final Calendar calendar = getCalendar();
        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(calendar);
        row.createCell(2).setCellValue(getCalendar(post.getReceiveTime()));
        row.createCell(3).setCellValue("*");
        row.createCell(4).setCellValue(post.getId());
        row.createCell(5).setCellValue("*");
        row.createCell(6).setCellValue("*");
        row.createCell(7).setCellValue(post.getNowLocation());
        row.createCell(8).setCellValue(getComplaintType(isDelivered));
        row.createCell(9).setCellValue(getPostStatus(post, postRoutes));
        row.createCell(10).setCellValue(getReply(isDelivered, calendar));
        row.createCell(11).setCellValue(getStatus(isDelivered));

        // Format row
        for (int j = 0; j < COLUMNS.length; ++j) {
            Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            if (j == 1 || j == 2) {
                cell.setCellStyle(getCellStyleDate(workbook));
            } else {
                cell.setCellStyle(getCommonStyle(workbook));
            }
        }
    }

    private void formatWorkbook(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);

        for (int i = 0; i < COLUMNS.length; ++i) {
            workbook.getSheetAt(0).autoSizeColumn(i);
        }
    }

    protected List<List<String>> sliceIds(List<String> ids) {
        List<List<String>> slices = new ArrayList<>(ids.size() / POST_QUERY_LIMIT + 1);
        for (int i = 0; i < ids.size(); i += POST_QUERY_LIMIT) {
            slices.add(ids.subList(i, Math.min(i + POST_QUERY_LIMIT, ids.size())));
        }
        return slices;
    }

    protected List<List<String>> loadIds(String filePath)
        throws IOException, InvalidFormatException {
        List<String> ids = loadFromFile(filePath).stream().distinct().collect(Collectors.toList());
        taskMax = ids.size();
        return sliceIds(ids);
    }

    protected void checkNetworkConnection() throws Exception {
        // Try loading cookies from disk
        try {
            QueryTask.loadCookiesFromFile(new File(COOKIE_FILE));
        } catch (IOException e) {
            logger.warning(
                "Could not load cookies from file, message is " + e.getLocalizedMessage());
        } catch (ClassNotFoundException e) {
            logger.warning("Could not load cookies from file, object isn't CookieStore? "
                + e.getLocalizedMessage());
        }

        // Check Internet and set cookie
        try {
            HttpResponse response = QueryTask.visitMainPage();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException(String.format(
                    "邮政网页访问异常(%d)!", response.getStatusLine().getStatusCode()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("网络异常，请检查你的网络连接！", e);
        }

        // Save cookies to disk
        try {
            QueryTask.saveCookiesToFile(new File(COOKIE_FILE));
        } catch (IOException e) {
            logger.warning("Could not save cookies to file, " + e.getLocalizedMessage());
        }
    }

    @Override
    protected File call() throws Exception {
        checkNetworkConnection();

        // Load and remove duplicates
        List<List<String>> idSlices = loadIds(filePath);
        if (idSlices.isEmpty()) {
            throw new Exception("文件中不存在运单号！");
        }

        updateProgress(0, taskMax);
        AtomicInteger counter = new AtomicInteger();
        Workbook workbook = newWorkbook();

        // Do it in parallel
        final ForkJoinPool pool = new ForkJoinPool(MAX_CONCURRENT_THREADS);
        pool.submit(() -> idSlices.parallelStream().forEach(slice -> {
                List<Post> posts = queryPosts(slice);
                try {
                    pool.submit(() -> posts.parallelStream().forEach(post -> {
                            List<PostRoute> routes = queryPostRoutes(post);
                            synchronized (workbook) {
                                writeToTable(workbook, post, routes);
                            }
                            updateProgress(counter.incrementAndGet(), taskMax);
                            if (counter.get() % 200 == 0) {
                                logger.info("Progress: " + counter.get() + "/" + taskMax);
                            }
                        }))
                        .get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.warning(
                        "Exception occurs when executing queries: " + e.getLocalizedMessage());
                }
            }))
            .get();

        formatWorkbook(workbook);
        // Write and return.
        File output = new File(outputPath(filePath));
        workbook.write(new FileOutputStream(output));

        return output;
    }
}
