package gov.nasa.jpl.mbee.systemsreasoner.validation.actions;

import java.util.ArrayList;
import java.util.List;

import gov.nasa.jpl.mbee.systemsreasoner.validation.GenericRuleViolationAction;

import com.nomagic.magicdraw.copypaste.CopyPasting;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.DataType;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.PrimitiveType;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.RedefinableElement;

public class CreateSpecializedTypeAction extends GenericRuleViolationAction {
	
	public static final List<Class<? extends Classifier>> UNSPECIALIZABLE_CLASSIFIERS = new ArrayList<Class<? extends Classifier>>();
	
	static {
		UNSPECIALIZABLE_CLASSIFIERS.add(DataType.class);
		UNSPECIALIZABLE_CLASSIFIERS.add(PrimitiveType.class);
	}

	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_NAME = "Create Specialized Classifier";
	
	private Property property;
	private Classifier parent;
	private boolean redefineAttributes;
	private String name;
	
	public CreateSpecializedTypeAction(final Property property, final Classifier parent) {
		this(property, parent, true, DEFAULT_NAME);
	}
	
	public CreateSpecializedTypeAction(final Property property, final Classifier parent, final boolean redefineAttributes, final String name) {
		super(name, name, null, null);
		this.property = property;
		this.parent = parent;
		this.redefineAttributes = redefineAttributes;
		this.name = name;
	}
	
	public static final void createSpecializedType(final Property property, final Classifier parent, final boolean redefineAttributes) {
		createSpecializedType(property, parent, redefineAttributes, new ArrayList<Property>());
	}
	
	public static final void createSpecializedType(final Property property, final Classifier parent, final boolean redefineAttributes, final List<Property> traveled) {
		if (!parent.isEditable()) {
			Application.getInstance().getGUILog().log(parent.getQualifiedName() + " is not editable. Skipping creating specialization.");
			return;
		}
		if (property.getType() instanceof Classifier && !(property.getType() instanceof Property)) {
			if (traveled.contains(property)) {
				Application.getInstance().getGUILog().log("Warning: Detected circular reference at " + property.getQualifiedName() + ". Stopping recursion.");
				return;
			}
			traveled.add(property);
			
			for (final Class<? extends Classifier> c : UNSPECIALIZABLE_CLASSIFIERS) {
				if (c.isAssignableFrom(property.getType().getClass())) {
					Application.getInstance().getGUILog().log("Warning: " + property.getQualifiedName() + " is a " + c.getSimpleName() + ", which is not specializable.");
					return;
				}
			}
			final Classifier general = (Classifier) property.getType();
			//System.out.println(general.getQualifiedName());
			final Classifier special = (Classifier) CopyPasting.copyPasteElement(general, parent);
			//special.getOwnedMember().clear();
			SpecializeClassifierAction.specialize(special, general);
			property.setType(special);
			if (redefineAttributes) {
				for (final NamedElement ne : special.getInheritedMember()) {
					if (ne instanceof Property && ne instanceof RedefinableElement && !((RedefinableElement) ne).isLeaf()) {
						RedefineAttributeAction.redefineAttribute(special, (RedefinableElement) ne, true, traveled);
					}
				}
			}
		}
	}

	@Override
	public void run() {
		CreateSpecializedTypeAction.createSpecializedType(property, parent, redefineAttributes);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSessionName() {
		return "create specialized classifier";
	}
}
