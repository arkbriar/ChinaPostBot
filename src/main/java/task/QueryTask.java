package task;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    private static final int POST_QUERY_LIMIT = 40;
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_CONCURRENT_THREADS = 16;
    private final Logger logger = Logger.getLogger(QueryTask.class.getName());
    private final String name;
    private final String filePath;
    private HttpClient httpClient = HttpClients.createDefault();

    public QueryTask(String name, String filePath) {
        this.name = name;
        this.filePath = filePath;
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

    private HttpResponse doPost(String url, List<NameValuePair> params) throws IOException {
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

    private Font getDefaultFont(Workbook workbook) {
        Font font = workbook.createFont();
        String osName = System.getProperty("os.name");
        switch (osName) {
            case "Windows":
                font.setFontName("楷体");
                break;
            default:
                font.setFontName("KaiTi");
                break;
        }
        font.setFontHeightInPoints((short) 10);
        return font;
    }

    private CellStyle getDefaultStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFont(getDefaultFont(workbook));
        return style;
    }

    private Workbook newWorkbook() {
        Workbook workbook = new XSSFWorkbook(XSSFWorkbookType.XLSX);
        Sheet mainSheet = workbook.createSheet("主动");

        final String[] columns = {
            "售后人员", "查询日期", "收寄日期", "客户", "运单号", "收件人",
            "联系方式", "地址", "投诉类别", "物流", "答复", "完成状态"
        };
        Row titleRow = mainSheet.createRow(0);

        for (int i = 0; i < columns.length; ++i) {
            titleRow.createCell(i).setCellValue(columns[i]);
        }

        // Set bold
        CellStyle style = workbook.createCellStyle();
        Font font = getDefaultFont(workbook);
        font.setBold(true);
        style.setFont(font);
        for (int i = 0; i < columns.length; ++i) {
            titleRow.getCell(i).setCellStyle(style);
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

        String name = "";
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

    private void writeToTable(Workbook workbook, Post post, List<PostRoute> postRoutes) {
        if (post == null || postRoutes == null || postRoutes.isEmpty()) {
            // ignore invalid cases
            return;
        }

        if (postRoutes.get(postRoutes.size() - 1).getType() != PostRoute.RouteType.RECEIPT) {
            logger.info(String.format("Ignore unsuccessful delivery %s!", post.getId()));
            return;
        }

        final Sheet sheet = workbook.getSheetAt(0);
        final Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        final Calendar calendar = getCalendar();
        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(calendar);
        row.createCell(2).setCellValue(getCalendar(post.getReceiveTime()));
        row.createCell(3).setCellValue("*");
        row.createCell(4).setCellValue(post.getId());
        row.createCell(5).setCellValue("*");
        row.createCell(6).setCellValue("*");
        row.createCell(7).setCellValue(post.getNowLocation());
        row.createCell(8).setCellValue("信息未更新，实际已妥投");

        String signedWho = getPersonSigned(postRoutes);
        String deliveryInfo = getLastArrival(postRoutes) + " 已妥投";
        if (!signedWho.isEmpty()) {
            deliveryInfo += "，签收人: " + signedWho;
        }
        if (post.getStatus().contains("704")) {
            deliveryInfo += " 704【转】";
        }
        row.createCell(9).setCellValue(deliveryInfo);

        row.createCell(10).setCellValue(getSimpleDateString(calendar) + "，已签收");
        row.createCell(11).setCellValue("已妥投");

        // Format cell
        for (int i = 0; i < 12; ++i) {
            Cell cell = row.getCell(i);
            CellStyle cellStyle = getDefaultStyle(workbook);
            if (i == 1 || i == 2) {
                cellStyle.setDataFormat(
                    workbook.getCreationHelper().createDataFormat().getFormat("mm月dd日"));
            }
            cell.setCellStyle(cellStyle);

            if (cell.getCellTypeEnum() == CellType.STRING) {
                cell.setCellValue(cell.getStringCellValue());
            }
        }
    }

    private List<Post> getPosts(List<String> ids) {
        updateMessage("查询运单号");

        List<Post> posts = new ArrayList<>(ids.size());

        for (int i = 0; i < ids.size(); i += POST_QUERY_LIMIT) {
            int batchSize = Math.min(POST_QUERY_LIMIT, ids.size() - i);
            posts.addAll(queryPosts(ids.subList(i, i + batchSize)));
            updateProgress(i + batchSize, ids.size());
        }

        assert posts.size() == ids.size();

        return posts;
    }

    private List<Post> getPostConcurrently(List<String> ids) throws InterruptedException {
        updateMessage("查询运单号");

        List<Post> posts = new ArrayList<>(ids.size());

        final ExecutorService executorService =
            Executors.newFixedThreadPool(MAX_CONCURRENT_THREADS);
        for (int i = 0; i < ids.size(); i += POST_QUERY_LIMIT) {
            int batchSize = Math.min(POST_QUERY_LIMIT, ids.size() - i);
            List<String> subIds = ids.subList(i, i + batchSize);
            executorService.submit(() -> {
                synchronized (posts) {
                    posts.addAll(queryPosts(subIds));
                    updateProgress(posts.size(), ids.size());
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

        assert ids.size() == posts.size();
        return posts;
    }

    private File queryRoutesAndWriteToExcelConcurrently(List<Post> posts)
        throws IOException, InterruptedException {
        updateMessage("查询运单路线");

        Workbook workbook = newWorkbook();

        final ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_THREADS);
        AtomicInteger count = new AtomicInteger();
        for (Post post : posts) {
            executorService.submit(() -> {
                List<PostRoute> routes = queryPostRoutes(post);
                synchronized (workbook) {
                    writeToTable(workbook, post, routes);
                    updateProgress(count.incrementAndGet(), posts.size());
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

        // Format workbook
        for (int i = 0; i < 13; ++i) {
            workbook.getSheetAt(0).autoSizeColumn(i);
        }

        // Write and return.
        File output = new File(outputPath(filePath));
        workbook.write(new FileOutputStream(output));

        return output;
    }

    private File queryRoutesAndWriteToExcel(List<Post> posts) throws IOException {
        updateMessage("查询运单路线");

        Workbook workbook = newWorkbook();

        for (int i = 0; i < posts.size(); ++i) {
            Post post = posts.get(i);
            List<PostRoute> routes = queryPostRoutes(post);
            writeToTable(workbook, post, routes);

            updateProgress(i + 1, posts.size());
        }

        // Format workbook
        for (int i = 0; i < 13; ++i) {
            workbook.getSheetAt(0).autoSizeColumn(i);
        }

        // Write and return.
        File output = new File(outputPath(filePath));
        workbook.write(new FileOutputStream(output));

        return output;
    }

    @Override
    protected File call() throws Exception {
        // Load and remove duplicates
        List<String> ids = loadFromFile(filePath).stream().distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            throw new Exception("文件中不存在运单号！");
        }

        updateProgress(0, ids.size());

        List<Post> posts;
        if (ids.size() > 4000) {
            posts = getPostConcurrently(ids);
        } else {
            posts = getPosts(ids);
        }

        updateProgress(0, ids.size());

        return queryRoutesAndWriteToExcelConcurrently(posts);
    }
}
