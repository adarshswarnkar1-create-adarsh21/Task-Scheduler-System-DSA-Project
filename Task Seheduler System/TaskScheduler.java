import java.io.*;
import java.util.*;

/** One task: id, title and priority (1 = High, 2 = Medium, 3 = Low). */
class Task {
    int id;
    String title;
    int priority;

    Task(int id, String title, int priority) {
        this.id = id;
        this.title = title;
        this.priority = priority;
    }

    /** True if this task must run before the other (lower priority number, then lower id). */
    boolean before(Task o) {
        return priority < o.priority || (priority == o.priority && id < o.id);
    }

    String priorityName() {
        return priority == 1 ? "High" : priority == 2 ? "Medium" : "Low";
    }

    public String toString() {
        return "[ID " + id + "] " + title + " (Priority: " + priorityName() + ")";
    }
}

/** Min-heap of tasks. Smaller priority number first; equal priority -> smaller id first. */
class TaskHeap {
    private Task[] data = new Task[8];
    private int size = 0;

    boolean isEmpty() { return size == 0; }

    int size() { return size; }

    private boolean less(Task x, Task y) {
        return x.before(y);
    }

    private void swap(int i, int j) {
        Task t = data[i]; data[i] = data[j]; data[j] = t;
    }

    private void siftUp(int i) {
        while (i > 0 && less(data[i], data[(i - 1) / 2])) {
            swap(i, (i - 1) / 2);
            i = (i - 1) / 2;
        }
    }

    private void siftDown(int i) {
        while (true) {
            int l = 2 * i + 1, r = 2 * i + 2, m = i;
            if (l < size && less(data[l], data[m])) m = l;
            if (r < size && less(data[r], data[m])) m = r;
            if (m == i) return;
            swap(i, m);
            i = m;
        }
    }

    void insert(Task t) {
        if (size == data.length) data = Arrays.copyOf(data, size * 2);
        data[size] = t;
        siftUp(size);
        size++;
    }

    Task extractMin() {
        Task top = data[0];
        data[0] = data[--size];
        data[size] = null;
        if (size > 0) siftDown(0);
        return top;
    }

    /** Removes the task with the given id. Returns it, or null if not found. */
    Task removeById(int id) {
        for (int i = 0; i < size; i++) {
            if (data[i].id == id) {
                Task removed = data[i];
                data[i] = data[--size];
                data[size] = null;
                if (i < size) { siftDown(i); siftUp(i); }
                return removed;
            }
        }
        return null;
    }

    /** Copy of all tasks in execution order (does not change the heap). */
    Task[] toSortedArray() {
        Task[] copy = Arrays.copyOf(data, size);
        TaskSorter.mergeSort(copy);
        return copy;
    }
}

/** One node of the completed-tasks queue. */
class TaskNode {
    Task task;
    TaskNode next;

    TaskNode(Task task) { this.task = task; }
}

public class TaskScheduler {
    private static final String FILE = "tasks.txt";

    private TaskHeap pending = new TaskHeap();
    private TaskQueue completed = new TaskQueue();
    private int nextId = 1;
    private Scanner in = new Scanner(System.in);

    void addTask(String title, int priority) {
        Task t = new Task(nextId++, title, priority);
        pending.insert(t);
        System.out.println("Task added. ID: " + t.id);
    }

    void executeNext() {
        if (pending.isEmpty()) {
            System.out.println("No pending tasks.");
            return;
        }
        Task t = pending.extractMin();
        completed.enqueue(t);
        System.out.println("Executing task: " + t);
    }

    void removeTask(int id) {
        Task t = pending.removeById(id);
        if (t == null) System.out.println("Task ID not found.");
        else System.out.println("Task removed: " + t);
    }

    void displayPending() {
        if (pending.isEmpty()) {
            System.out.println("No pending tasks.");
            return;
        }
        int no = 1;
        for (Task t : pending.toSortedArray())
            System.out.println(no++ + ". " + t);
    }

    void displayCompleted() {
        if (completed.isEmpty()) System.out.println("No completed tasks.");
        else completed.display();
    }

    void loadFromFile() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|", 3);
                if (p.length < 3) continue;
                int id = Integer.parseInt(p[0].trim());
                int pr = Integer.parseInt(p[1].trim());
                pending.insert(new Task(id, p[2], pr));
                if (id >= nextId) nextId = id + 1;
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Could not read " + FILE + " fully.");
        }
    }

    void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            for (Task t : pending.toSortedArray())
                pw.println(t.id + "|" + t.priority + "|" + t.title);
            System.out.println("Saved to " + FILE + ". Goodbye!");
        } catch (IOException e) {
            System.out.println("Error: could not save " + FILE);
        }
    }

    private int readInt(String prompt) {
        System.out.print(prompt);
        String s = in.hasNextLine() ? in.nextLine().trim() : "6";
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return -1; }
    }

    void run() {
        loadFromFile();
        int choice = 0;
        while (choice != 6) {
            System.out.println("\n1 Add task | 2 Execute next task | 3 View pending"
                    + " | 4 View completed | 5 Remove task | 6 Exit");
            choice = readInt("Enter choice: ");
            switch (choice) {
                case 1:
                    System.out.print("Title: ");
                    String title = in.hasNextLine() ? in.nextLine().trim().replace('|', ' ') : "";
                    if (title.isEmpty()) { System.out.println("Title cannot be empty."); break; }
                    int pr = readInt("Priority (1 = High, 2 = Medium, 3 = Low): ");
                    if (pr < 1 || pr > 3) { System.out.println("Invalid priority. Choose 1 to 3."); break; }
                    addTask(title, pr);
                    break;
                case 2: executeNext(); break;
                case 3: displayPending(); break;
                case 4: displayCompleted(); break;
                case 5:
                    int id = readInt("Task ID to remove: ");
                    if (id < 1) System.out.println("Invalid ID.");
                    else removeTask(id);
                    break;
                case 6: saveToFile(); break;
                default: System.out.println("Invalid option. Choose 1 to 6.");
            }
        }
    }

    public static void main(String[] args) {
        new TaskScheduler().run();
    }
}

/** Merge sort for tasks: stable, O(n log n). Used to list pending tasks in execution order. */
class TaskSorter {
    static void mergeSort(Task[] a) {
        if (a.length < 2) return;
        sort(a, new Task[a.length], 0, a.length - 1);
    }

    private static void sort(Task[] a, Task[] tmp, int lo, int hi) {
        if (lo >= hi) return;
        int mid = (lo + hi) / 2;
        sort(a, tmp, lo, mid);
        sort(a, tmp, mid + 1, hi);
        int i = lo, j = mid + 1, k = lo;
        while (i <= mid && j <= hi) tmp[k++] = a[j].before(a[i]) ? a[j++] : a[i++];
        while (i <= mid) tmp[k++] = a[i++];
        while (j <= hi) tmp[k++] = a[j++];
        for (k = lo; k <= hi; k++) a[k] = tmp[k];
    }
}

/** FIFO queue of executed tasks (front = first executed, rear = last executed). Linked nodes. */
class TaskQueue {
    private TaskNode front, rear;
    private int size = 0;

    void enqueue(Task t) {
        TaskNode n = new TaskNode(t);
        if (rear == null) front = rear = n;
        else { rear.next = n; rear = n; }
        size++;
    }

    Task dequeue() {
        if (front == null) return null;
        Task t = front.task;
        front = front.next;
        if (front == null) rear = null;
        size--;
        return t;
    }

    boolean isEmpty() { return front == null; }

    void display() {
        int no = 1;
        for (TaskNode p = front; p != null; p = p.next)
            System.out.println(no++ + ". " + p.task);
    }
}