//This is Seth Barrett's TaskCB.java for Problem Set 3 of OS CSCI 3271 from OSP2. These are all my methods and my documentation.



package osp.Tasks;

import osp.IFLModules.*;
import osp.Threads.*;
import osp.Ports.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Utilities.*;
import osp.Hardware.*;
import java.util.ArrayList;
//I import java's ArrayList package to help the TCB(Task Control Block) maintain inventory of objects that a task will need to manage, utilize and control.

/*
    TCB Class Declaration
    I start my TCB class with the declaration of 3 collections to be used only in the task control block class to control account of the ports, open files and threads used by the thread.
*/
public class TaskCB extends IflTaskCB
{
    private ArrayList<PortCB> portL;
    //Declaring private ArrayList object named portL,with elements PortCB, which means it is a collection and an interable list that can contain port objects used by the TCB.

    private ArrayList<OpenFile> oFileL;
    //Declaring private ArrayList object named oFileL with elements OpenFiles meaning that it contain open files used by the TCB.

    private ArrayList<ThreadCB> threadL;
    //Declaring private ArrayList object named threadL containing threads used by the TCB.

    /*
        TCB Class Constructor
        This must start with super() followed by the assignment of the the three ArrayList variables to new ArrayLists values, each with the same class as it's declaration above. These are going to contain lists to maintain inventory of communication ports, files and threads and they will be used by this class. The ArrayLists will be empty until do_create is used.
    */
    public TaskCB()
    {
        super();
        //This call refers to extended class and must be called.

        portL = new ArrayList<PortCB>();
        //Assigns portL a new ArrayList object to contain elements of class PortCB that is empty with an intial capacity of 10 communication ports, which can be expanded at need. 

        oFileL = new ArrayList<OpenFile>();
        //Assigns oFileL a new empty ArrayList object to contain elements of class OpenFile used by the TCB.

        threadL = new ArrayList<ThreadCB>();
        //Assigns threadL a new empty ArrayList object to contain elements of class ThreadCB used by the TCB.
    }

    /*
        init()
        This method is called once at the beginning of the simulation. Can be used to initialize static variables. It is empty because I do not initalize any.
    */
    public static void init(){}

    /*
        do_create()
        The do_create is one of the primary methods of the TaskCB class. The priamry purpose of the do_create method is to creating a new task object set it up properly, and adequetely allocate resources through the usuage of the previously declared arraylist objects as well as a page table. A swap file is created, and then is checked to make sure there were no file creation errors. If any are found, a new thread is dispatched and null is returned. If there aren't any errors, an open-file handle containing the maximal number of bytes in the newly created virtual address space is saved in the task data structure tk, the first live thread is created and finally TaskCB object tk is returned as it has been completly created.
    */
    static public TaskCB do_create()
    {
        TaskCB tk = new TaskCB();
        //Creates a new TaskCB object named tk to deal with the creation and killing of tasks.

        PageTable pgTb = new PageTable(tk);
        //Creates a new PageTable named pgTb. 

        tk.setCreationTime(HClock.get());
        //Uses class HClock to set tk's task-creation time to be equal to current simulation time.

        tk.setPageTable(pgTb);
        //Associates page table object pgTb with TaskCB object tk.

        tk.setStatus(TaskLive);
        //Sets TaskLive as tk's status.

        tk.setPriority(5);
        //Sets tk's priority to 5. It says some int so kept trying different numbers till this worked.

        String swapFile = SwapDeviceMountPoint + tk.getID();
        //Gets path for file to be swapped as a String by combining the folder's path that the file will reside in from global constant SwapDeviceMountPoint and the name of the swap file, which is tk's ID number.

        int sFSize = (int)Math.pow(2, MMU.getVirtualAddressBits());
        //Sets integer sFSize to 2^(MMU's Virtual Address Bits) which equals the maximal number of bits needed to specify an address in the virtual address space of a task.

        FileSys.create(swapFile, sFSize);
        //Uses static method create() that takes the parameters of the swapFile, swap file's path and sFSize, the virtual address space of a task to create the swap file.

        OpenFile swapF = OpenFile.open(swapFile, tk);
        //Sets OpenFile object swapF by mode of static method open(), which took parameters swapFile, representing the full path name of the swap file, and TaskCB tk to allow close, write and read operations through the usage of a returned run-time file handle. 

        if (swapF == null)
        //If statement deals with errors of file creation when swapF is null becuase of the failure of the open() operation when there is insufficent space on a swap device. 
        {
            ThreadCB.dispatch();
            //A new thread is dispatched when file opening errors occur through the usage of the dispatch method of the ThreadCB class.

            return null;
            //Returns null and stops do_create method if swapF is null.
        }
        
        else 
        //Else statement contains statments for when there are no errors in file creation.
        {
            tk.setSwapFile(swapF);
            //Saves the open-file handle swapF to task tk.

            ThreadCB.create(tk);
            //Uses static method create() to create the first thread as OSP2 requires atleast one live thread.

            return tk;
            //Returns tk now that is finally created and intialized.
        }
        
    }

    /*
       do_kill()
       This is the other primary method of the TaskCB class and is used to destory a task by iterating through all the ArrayLists containing threads, communication ports and files and stoping them through each object class' appropiate method. The status of the task is also set to terminated, memory allocated for the task is released, and the swap file is destroyed.
    */
    public void do_kill()
    {
        while (threadL.size() > 0)
        //Iterated through threadL by comparing the size() method's return to 0. Iteration is possible because the kill() causes the size() method's return to decrement by one each call.

        threadL.get(0).kill();
        //Uses get(0) to affect the first value in the thread ArrayList and kill() to removed every thread and remove it from threadL. When size() is greater than zero, get(0) will always returns a valid thread object to be killed.

        while (portL.size() > 0) 
        //Iterated through portL by comparing the size() method's return to 0. Iteration is possible because the destroy() causes the size() method's return to decrement by one each call.

        portL.get(0).destroy();
        //Uses get(0) to affect the first value in the communication port ArrayList and destory() to remove every communication port and remove it from portL. When size() is greater than zero, get(0) will always returns a valid PortCB object to be killed.

        for (int i = oFileL.size() - 1; i >= 0; i--)
        //For loop is used because the close() method from the file class does not immediately remove the file from the ArrayList as the do_removeFile() maybe delayed.

        if (oFileL.get(i) != null)
        //Checks to make sure that the file oFile.get(i) was not null before executing the close() method.

        oFileL.get(i).close();
        //Uses get(i) to affect every value in the open file ArrayList and close() to remove the file, but this doesn't happen immediately as the file maybe being used elsewhere.

        this.setStatus(TaskTerm);
        //Sets TaskTerm to the status of the task to show that the task is terminated.

        this.getPageTable().deallocateMemory();
        //Uses the deallocateMemory() method from the PageTable class to release the task's previously allocated memory.

        FileSys.delete(SwapDeviceMountPoint + this.getID());
        //Uses the delete() method from the FileSys class to destory the swap file of the task by taking the parameter of the name of the swap file through SwapDeviceMountPoint and the getId() method.
    }

    /* 
        do_getThreadCount()
        This method returns an integer value representing the count of the number of threads in ArrayList threadL.
    */
    public int do_getThreadCount()
    {
        return threadL.size();
        //Uses the size method to return the number of threads.
    }

    /*
        do_addThread(ThreadCB thread)
        This method returns FAILURE if the max number of threads for this task has been created and SUCCESS elsewise. It is called elswhere in OSP2 and is used primarily for inventory control and managment of threads for this task. A thread object will not be added to ArrayList threadL if the thread count equals the max number of threads per task.
    */
    public int do_addThread(ThreadCB thread)
    {
        if (do_getThreadCount() < ThreadCB.MaxThreadsPerTask)
        //Uses if statement to check to see if the thread count is not equal to the max number of threads to make sure a thread can be added before continuing.
        {
            this.threadL.add(thread);
            return SUCCESS;
            //Adds the thread provided in the parameter of this method to ArrayList threadL and returns SUCCESS.
        }
        else return FAILURE; 
        //Returns FAILURE if the thread count is equal to the max number of threads.
    }

    /*
        do_removeThread(ThreadCB thread)
        Called when thread is destroyed, returns FAILURE if thread does not belong to threadL or there are no threads in threadL, removes the thread given in the parameter and returns SUCCESS elsewise.
    */
    public int do_removeThread(ThreadCB thread)
    {
        if (threadL.size() == 0) return FAILURE;
        //Returns FAILURE if no threads left in threadL.

        else if (threadL.contains(thread)) 
        {
            threadL.remove(thread);
            return SUCCESS;
            //Uses the remove method to remove thread and returns SUCCESS if thread in threadL.
        }
        else return FAILURE;
        //Returns FAILURE if thread not in threadL.
    }

    /*
       do_getPortCount()
       This method returns an integer value representing the count of the number of ports in ArrayList portL.
    */
    public int do_getPortCount()
    {
        return portL.size();
        //Uses the size method to return the number of communication ports.
    }

    /*
        do_addPort(PortCB newPort)
        This method returns FAILURE if the max number of portss for this task has been created and SUCCESS elsewise. It is called elswhere in OSP2 and is used primarily for inventory control and managment of portss for this task. A port object will not be added to ArrayList portL if the port count equals the max number of ports per task.
    */ 
    public int do_addPort(PortCB newPort)
    {
        if (do_getPortCount() < PortCB.MaxPortsPerTask)
        //Uses if statement to check to see if the port count is not equal to the max number of ports to make sure a port can be added before continuing.
        {
            portL.add(newPort);
            return SUCCESS;
            //Adds the port provided in the parameter of this method to ArrayList portL0 returns SUCCESS.
        }
        else return FAILURE;
        //Returns FAILURE if the port count is equal to the max number of ports.
    }

    /*
      do_removePort(PortCB oldPort)
      Called when port is removed, returns FAILURE if port does not belong to portL or there are no ports in portL, removes the port given in the parameter and returns SUCCESS elsewise.
    */ 
    public int do_removePort(PortCB oldPort)
    {
        if (portL.size() == 0) return FAILURE;
        //Returns FAILURE if no ports left in portL.

        else if (portL.contains(oldPort))
        {
            portL.remove(oldPort);
            return SUCCESS;
            //Uses the remove method to remove port and returns SUCCESS if port in portL.
        }
        else return FAILURE;
        //Returns FAILURE if port not in portL.
    }

    /*
       do_addFile(OpenFile file)
       This method adds an open file to the oFileL Array List, and does not return anything.
    */
    public void do_addFile(OpenFile file)
    {
        oFileL.add(file);
        //Adds file given as the parameter for the method tp the oFileL ArrayList.
    }

    /*
        dp_removeFile(OpenFile file)
        Called when file needs to be removed from the table of open files for the task, returns FAILURE if file does not belong to portL or there are no files in oFileL, removes the file given in the parameter and returns SUCCESS elsewise.

    */
    public int do_removeFile(OpenFile file)
    {
        if (oFileL.size() == 0) return FAILURE;
        //Returns FAILURE if no files left in oFileL.

        else if (oFileL.contains(file))
        {
            oFileL.remove(file);
            return SUCCESS;
            //Uses the remove methid to remove file and returns SUCCESS if file not in oFileL
        }
        else return FAILURE;
        //Returns FAILURE if file not in oFileL.
    }

    /*
       atError()
    */
    public static void atError()
    {
        System.out.println("Error");
    }

    /*
        atWarning()
    */
    public static void atWarning()
    {
        System.out.println("Warning");
    }
}

