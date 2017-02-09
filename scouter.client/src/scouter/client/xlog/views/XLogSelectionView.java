/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouter.client.xlog.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.model.XLogData;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.sorter.TableLabelSorter;
import scouter.client.util.ColorUtil;
import scouter.client.util.ImageUtil;
import scouter.client.xlog.actions.OpenXLogProfileJob;
import scouter.util.FormatUtil;
import scouter.util.Hexa32;
import scouter.util.IPUtil;
import scouter.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;


public class XLogSelectionView extends ViewPart {
	public static final String ID = XLogSelectionView.class.getName();

	private TableViewer viewer;
	private TableColumnLayout tableColumnLayout;
	private String yyyymmdd;
	private Clipboard clipboard;
	
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		Composite comp = new Composite(parent, SWT.NONE);
		tableColumnLayout = new TableColumnLayout();
		comp.setLayout(tableColumnLayout);
		viewer = new TableViewer(comp, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		createColumns();
		final Table table = viewer.getTable();
	    table.setHeaderVisible(true);
	    table.setLinesVisible(true);
	    viewer.setContentProvider(new ArrayContentProvider());
	    viewer.setComparator(new TableLabelSorter(viewer));
	    table.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StructuredSelection sel = (StructuredSelection) viewer.getSelection();
				Object o = sel.getFirstElement();
				if (o instanceof XLogData) {
					XLogData data = (XLogData) o;
					Display display = XLogSelectionView.this.getViewSite().getShell().getDisplay();
					new OpenXLogProfileJob(display, data, data.serverId).schedule();
				} else {
					System.out.println(o);
				}
			}
		});
	    IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
	    man.add(new Action("Copy to clipboard", ImageUtil.getImageDescriptor(Images.copy)){ 
	        public void run(){    
	        	copyToClipboard();
	        }
	    });
	    clipboard = new Clipboard(null);

		getSite().getPage().addPartListener(new org.eclipse.ui.IPartListener2()
		{
			@Override
			public void partActivated(IWorkbenchPartReference partRef) {
				System.out.println("#### partActivated");
			}
			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {
				System.out.println("#### partBroughtToTop");
			}
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				System.out.println("#### partBroughtToTop");
			}
			@Override
			public void partDeactivated(IWorkbenchPartReference partRef) {
				System.out.println("#### partDeactivated");

			}
			@Override
			public void partOpened(IWorkbenchPartReference partRef) {
				System.out.println("#### partOpened");
			}
			@Override
			public void partHidden(IWorkbenchPartReference partRef) {
				System.out.println("#### partHidden");
			}
			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				System.out.println("#### partVisible");
			}
			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {
				System.out.println("#### partInputChanged");
			}
		});
	}
	
	public void setInput(final ArrayList<XLogData> xperfData, String objType, final String yyyymmdd) {
		this.yyyymmdd = yyyymmdd;
		Server server = ServerManager.getInstance().getServer(xperfData.get(0).serverId);
		String objTypeDisplay = "";
		if(server != null){
			objTypeDisplay = server.getCounterEngine().getDisplayNameObjectType(objType);
		}
		setPartName(objTypeDisplay + " - " + "XLog List");
		viewer.setInput(xperfData);
	}
	
	public void setFocus() {
	}
	
	ArrayList<XLogColumnEnum> columnList = new ArrayList<XLogColumnEnum>();

	private void createColumns() {
		for (XLogColumnEnum column : XLogColumnEnum.values()) {
			createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			columnList.add(column);
		}
		viewer.setLabelProvider(new TableItemProvider());
	}
	
	class TableItemProvider implements ITableLabelProvider, IColorProvider {

		public Color getForeground(Object element) {
			if (element instanceof XLogData) {
				XLogData d = (XLogData) element;
				if (d.p.error != 0) {
					return ColorUtil.getInstance().getColor("red");
				}
			}
			return null;
		}

		public Color getBackground(Object element) {
			return null;
		}

		public void addListener(ILabelProviderListener listener) {
			
		}

		public void dispose() {
			
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void removeListener(ILabelProviderListener listener) {
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof XLogData == false) {
				return null;
			} 
			XLogData d = (XLogData) element;
			XLogColumnEnum column = columnList.get(columnIndex);
			switch (column) {
			case OBJECT :
				if (StringUtil.isEmpty(d.objName)) {
					d.objName = TextProxy.object.getLoadText(yyyymmdd, d.p.objHash, d.serverId);
				}
				return d.objName;
			case ELAPSED :
				return FormatUtil.print(d.p.elapsed, "#,##0");
			case SERVICE :
				if (StringUtil.isEmpty(d.serviceName)) {
					d.serviceName = TextProxy.service.getLoadText(yyyymmdd, d.p.service, d.serverId);
				}
				return d.serviceName;
			case START_TIME :
				return FormatUtil.print(new Date(d.p.endTime - d.p.elapsed), "HH:mm:ss.SSS");
			case END_TIME :
				return FormatUtil.print(new Date(d.p.endTime), "HH:mm:ss.SSS");
			case TX_ID :
				return Hexa32.toString32(d.p.txid);
			case CPU :
				return FormatUtil.print(d.p.cpu, "#,##0");
			case SQL_COUNT :
				return FormatUtil.print(d.p.sqlCount, "#,##0");
			case SQL_TIME :
					return FormatUtil.print(d.p.sqlTime, "#,##0");
			case KBYTES :
					return FormatUtil.print(d.p.kbytes, "#,##0");
			case IP :
					return IPUtil.toString(d.p.ipaddr);
			case ERROR :
				return d.p.error == 0 ? "" : TextProxy.error.getLoadText(yyyymmdd, d.p.error, d.serverId);
			case GX_ID :
				return Hexa32.toString32(d.p.gxid);
			case LOGIN :
				return d.p.login != 0 ? TextProxy.login.getLoadText(yyyymmdd, d.p.login, d.serverId) : null;
			case DESC :
				return d.p.desc != 0 ? TextProxy.desc.getLoadText(yyyymmdd, d.p.desc, d.serverId) : null;
			case DUMP :
				return d.p.hasDump == 1 ? "Y" : null;
			}
			return null;
		}
	}
	
	private TableViewerColumn createTableViewerColumn(String title, int width, int alignment,  boolean resizable, boolean moveable, final boolean isNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setAlignment(alignment);
		column.setMoveable(moveable);
		tableColumnLayout.setColumnData(column, new ColumnPixelData(width, resizable));
		column.setData("isNumber", isNumber);
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TableLabelSorter sorter = (TableLabelSorter) viewer.getComparator();
				TableColumn selectedColumn = (TableColumn) e.widget;
				sorter.setColumn(selectedColumn);
			}
		});
		return viewerColumn;
	}
	
	private void copyToClipboard() {
		if (viewer != null) {
			TableItem[] items = viewer.getTable().getItems();
			int colCnt = viewer.getTable().getColumnCount();
			if (items != null && items.length > 0) {
				StringBuffer sb = new StringBuffer();
				for (TableItem item : items) {
					for (int i = 0; i < colCnt; i++) {
						sb.append(item.getText(i));
						if (i == colCnt - 1) {
							sb.append("\n");
						} else {
							sb.append("\t");
						}
					}
				}
				clipboard.setContents(new Object[] {sb.toString()}, new Transfer[] {TextTransfer.getInstance()});
				MessageDialog.openInformation(getSite().getShell(), "Copy", "Copied to clipboard");
			}
		}
	}
	
	enum XLogColumnEnum {
	    OBJECT("Object", 80, SWT.LEFT, true, true, false),
	    ELAPSED("Elapsed", 50, SWT.RIGHT, true, true, true),
	    SERVICE("Service", 100, SWT.LEFT, true, true, false),
	    END_TIME("EndTime", 70, SWT.CENTER, true, true, true),
		CPU("Cpu", 40, SWT.RIGHT, true, true, true),
		SQL_COUNT("SQL Count", 50, SWT.RIGHT, true, true, true),
		SQL_TIME("SQL Time", 50, SWT.RIGHT, true, true, true),
		KBYTES("KBytes", 60, SWT.RIGHT, true, true, true),
		IP("IP", 90, SWT.LEFT, true, true, false),
		LOGIN("Login", 50, SWT.LEFT, true, true, false),
		DUMP("Dump", 40, SWT.CENTER, true, true, false),
		ERROR("Error", 50, SWT.LEFT, true, true, false),
		TX_ID("Txid", 30, SWT.LEFT, true, true, false),
		GX_ID("Gxid", 30, SWT.LEFT, true, true, false),
		DESC("Desc", 50, SWT.LEFT, true, true, false),
		START_TIME("StartTime", 70, SWT.CENTER, true, true, true);

	    private final String title;
	    private final int weight;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private XLogColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
	        this.title = text;
	        this.weight = width;
	        this.alignment = alignment;
	        this.resizable = resizable;
	        this.moveable = moveable;
	        this.isNumber = isNumber;
	    }
	    
	    public String getTitle(){
	        return title;
	    }

	    public int getAlignment(){
	        return alignment;
	    }

	    public boolean isResizable(){
	        return resizable;
	    }

	    public boolean isMoveable(){
	        return moveable;
	    }

		public int getWidth() {
			return weight;
		}
		
		public boolean isNumber() {
			return this.isNumber;
		}
	}

	public void dispose() {
		clipboard.dispose();
		super.dispose();
	}
}
