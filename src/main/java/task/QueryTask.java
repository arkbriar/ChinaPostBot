package task;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import model.Post;
import model.PostRoute;

/**
 * Created by Shunjie Ding on 31/12/2017.
 */
public class QueryTask implements Callable<File> {
    private static final int POST_QUERY_LIMIT = 40;

    private final String name;

    private final String filePath;

    private final ProgressUpdater updater;

    public QueryTask(String name, String filePath, ProgressUpdater updater) {
        this.name = name;
        this.filePath = filePath;
        this.updater = updater;
    }

    private List<String> loadFromFile(String filePath) {
        // TODO
        // Workbook workbook = WorkbookFactory
        return new ArrayList<>();
    }

    private List<Post> queryPosts(List<String> ids) {
        // TODO
        // TODO Retry one more time on failure
        return new ArrayList<>();
    }

    private List<PostRoute> queryPostRoutes(Post post) {
        // TODO
        // TODO Retry one more time on failure
        return new ArrayList<>();
    }

    private String outputPath(String filePath) {
        File file = new File(filePath);
        String filename = file.getName();
        int idx = filename.lastIndexOf('.');
        assert idx != -1;

        return Paths.get(file.getParent(), filename.substring(0, idx) + "_结果.xlsx").toString();
    }

    private Workbook newWorkbook() {
        Workbook workbook = new XSSFWorkbook(XSSFWorkbookType.XLSX);
        Sheet mainSheet = workbook.createSheet("主动");

        final String[] columns = {"售后人员", "查询日期", "收寄日期", "客户", "运单号", "收件人",
            "联系方式", "地址", "投诉类别", "物流", "答复", "完成状态"};
        Row titleRow = mainSheet.createRow(0);
        for (int i = 0; i < columns.length; ++i) {
            titleRow.createCell(i).setCellValue(columns[i]);
        }

        return workbook;
    }

    private void writeToTable(Workbook workbook, Post post, List<PostRoute> postRoutes) {
        // TODO
    }

    @Override
    public File call() throws Exception {
        List<String> ids = loadFromFile(filePath);

        Workbook workbook = newWorkbook();

        for (int i = 0; i < ids.size(); i += POST_QUERY_LIMIT) {
            List<Post> posts =
                queryPosts(ids.subList(i, Math.min(i + POST_QUERY_LIMIT, ids.size())));

            for (Post post : posts) {
                List<PostRoute> routes = queryPostRoutes(post);
                writeToTable(workbook, post, routes);
            }
            updater.update((double) i / (double) (ids.size()));
        }

        // Write and return.
        File output = new File(outputPath(filePath));
        workbook.write(new FileOutputStream(output));
        return output;
    }
}
