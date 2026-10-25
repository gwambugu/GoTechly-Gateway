export interface PluginInfo {
  jarFileName: string;
  paymentTypes: string[];
  handlers: HandlerInfo[];
}

export interface HandlerInfo {
  paymentType: string;
  handlerClassName: string;
  handlerType: string;
  version: string;
  mode: string;
}
