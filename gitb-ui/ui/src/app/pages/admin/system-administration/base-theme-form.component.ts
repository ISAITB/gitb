import { BaseComponent } from '../../base-component.component';
import { Theme } from 'src/app/types/theme';

export abstract class BaseThemeFormComponent extends BaseComponent {

  originalPrimaryButtonColor?: string
  originalSecondaryButtonColor?: string

  protected processButtonColors(theme: Theme) {
    if (theme.primaryButtonColor != this.originalPrimaryButtonColor) {
      theme.primaryButtonHoverColor = theme.primaryButtonColor
      theme.primaryButtonActiveColor = theme.primaryButtonColor
    }
    if (theme.secondaryButtonColor != this.originalSecondaryButtonColor) {
      theme.secondaryButtonHoverColor = theme.secondaryButtonColor
      theme.secondaryButtonActiveColor = theme.secondaryButtonColor
    }
  }

}
