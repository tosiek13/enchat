/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package plugin.plugins;

/**
 *
 * @author robert
 */
public class MScreenPlugin extends Plugin
{
    @Override
    public void reset()
    {
        // do nothing
    }

    @Override
    public void update(int error, String[] parameters)
    {
        pluginManager.updateControllerError(error);
        pluginManager.switchDisplayToMain();
    }
}
